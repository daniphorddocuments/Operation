document.addEventListener('DOMContentLoaded', function () {
    const originalFetch = window.fetch.bind(window);

    function readCookie(name) {
        const prefix = name + '=';
        return document.cookie.split(';').map(function (item) {
            return item.trim();
        }).filter(function (item) {
            return item.startsWith(prefix);
        }).map(function (item) {
            return decodeURIComponent(item.substring(prefix.length));
        })[0] || '';
    }

    window.fetch = function (resource, options) {
        const requestOptions = options ? Object.assign({}, options) : {};
        const method = String(requestOptions.method || 'GET').toUpperCase();
        const headers = new Headers(requestOptions.headers || {});
        if (!['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(method)) {
            const csrfToken = readCookie('XSRF-TOKEN');
            if (csrfToken && !headers.has('X-XSRF-TOKEN')) {
                headers.set('X-XSRF-TOKEN', csrfToken);
            }
        }
        requestOptions.headers = headers;
        return originalFetch(resource, requestOptions);
    };

    const context = window.FROMS_PUBLIC_REPORT || {};
    if (!context.reportNumber || !context.token) {
        return;
    }

    const chatFeed = document.getElementById('publicChatFeed');
    const chatForm = document.getElementById('publicChatForm');
    const chatMessage = document.getElementById('publicChatMessage');
    const publicLocalVideo = document.getElementById('publicLocalVideo');
    const publicRemoteAudio = document.getElementById('publicRemoteAudio');
    const publicVideoStatus = document.getElementById('publicVideoStatus');
    const publicStartVideo = document.getElementById('publicStartVideo');
    const publicEndVideo = document.getElementById('publicEndVideo');
    const publicAudioMode = document.getElementById('publicAudioMode');
    const publicToggleAudio = document.getElementById('publicToggleAudio');

    let socket = null;
    let signalReadyPromise = null;
    let peerConnection = null;
    let localStream = null;
    let currentVideoSessionId = null;
    let callerAudioEnabled = true;
    const mediaConstraints = {
        video: {
            facingMode: { ideal: 'environment' },
            width: { ideal: 1280, max: 1920 },
            height: { ideal: 720, max: 1080 },
            frameRate: { ideal: 24, max: 30 }
        },
        audio: {
            echoCancellation: true,
            noiseSuppression: true,
            autoGainControl: true
        }
    };

    async function readPayload(response) {
        const text = await response.text();
        if (!text) {
            return {};
        }
        try {
            return JSON.parse(text);
        } catch (error) {
            throw new Error('Server returned an unexpected response. Please try again.');
        }
    }

    function createEmptyStateNode(text) {
        const node = document.createElement('div');
        node.className = 'small ui-muted';
        node.textContent = text;
        return node;
    }

    function createListRow(sender, message, timestamp) {
        const row = document.createElement('div');
        row.className = 'ui-list-row';

        const content = document.createElement('div');
        const title = document.createElement('div');
        title.className = 'fw-semibold';
        title.textContent = sender;
        const subtitle = document.createElement('div');
        subtitle.className = 'small ui-muted';
        subtitle.textContent = message;
        content.appendChild(title);
        content.appendChild(subtitle);

        const trailing = document.createElement('strong');
        trailing.textContent = timestamp;

        row.appendChild(content);
        row.appendChild(trailing);
        return row;
    }

    function createSkeletonRow() {
        const row = document.createElement('div');
        row.className = 'ui-list-row ui-skeleton-row';
        const content = document.createElement('div');
        content.className = 'ui-skeleton-stack';
        const linePrimary = document.createElement('div');
        linePrimary.className = 'ui-skeleton-block ui-skeleton-block-lg';
        const lineSecondary = document.createElement('div');
        lineSecondary.className = 'ui-skeleton-block';
        const trailing = document.createElement('div');
        trailing.className = 'ui-skeleton-block ui-skeleton-block-sm';
        content.appendChild(linePrimary);
        content.appendChild(lineSecondary);
        row.appendChild(content);
        row.appendChild(trailing);
        return row;
    }

    function renderLoadingSkeleton(count) {
        if (!chatFeed) {
            return;
        }
        const fragment = document.createDocumentFragment();
        for (let index = 0; index < count; index += 1) {
            fragment.appendChild(createSkeletonRow());
        }
        chatFeed.replaceChildren(fragment);
    }

    function renderMessages(messages) {
        if (!chatFeed) {
            return;
        }
        const fragment = document.createDocumentFragment();
        if (!Array.isArray(messages) || messages.length === 0) {
            fragment.appendChild(createEmptyStateNode('No messages yet.'));
        } else {
            messages.forEach(function (item) {
                fragment.appendChild(createListRow(item.senderType || 'PUBLIC', item.message || '', item.createdAt || ''));
            });
        }
        chatFeed.replaceChildren(fragment);
    }

    async function loadMessages() {
        renderLoadingSkeleton(3);
        const response = await window.fetch(context.reportMessagesUrl, {
            method: 'GET',
            credentials: 'same-origin'
        });
        const payload = await readPayload(response);
        if (!response.ok) {
            throw new Error(payload.error || 'Unable to load messages');
        }
        renderMessages(payload);
    }

    if (chatForm) {
        chatForm.addEventListener('submit', function (event) {
            event.preventDefault();
            window.fetch(context.reportMessagesUrl, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: chatMessage.value })
            }).then(async function (response) {
                const payload = await readPayload(response);
                if (!response.ok) {
                    throw new Error(payload.error || 'Unable to send message');
                }
                chatMessage.value = '';
                return loadMessages();
            }).catch(function (error) {
                window.alert(error.message);
            });
        });
    }

    function updateVideoStatus(text) {
        if (publicVideoStatus) {
            publicVideoStatus.textContent = text;
        }
    }

    function syncCallerAudioState() {
        if (localStream) {
            localStream.getAudioTracks().forEach(function (track) {
                track.enabled = callerAudioEnabled;
            });
        }
        if (publicToggleAudio) {
            publicToggleAudio.textContent = callerAudioEnabled ? 'Mute Voice' : 'Allow Voice';
        }
    }

    function attachStream(videoElement, stream) {
        if (!videoElement) {
            return;
        }
        videoElement.srcObject = stream;
        const tryPlay = function () {
            const playPromise = videoElement.play();
            if (playPromise && typeof playPromise.catch === 'function') {
                playPromise.catch(function () {});
            }
        };
        videoElement.onloadedmetadata = tryPlay;
        tryPlay();
    }

    function stopStream(stream) {
        if (!stream) {
            return;
        }
        stream.getTracks().forEach(function (track) {
            track.stop();
        });
    }

    async function getPreferredMediaStream(audioEnabled) {
        const stream = await navigator.mediaDevices.getUserMedia(mediaConstraints);
        stream.getVideoTracks().forEach(function (track) {
            try {
                track.contentHint = 'detail';
            } catch (error) {
                console.warn('Unable to apply video detail hint', error);
            }
        });
        stream.getAudioTracks().forEach(function (track) {
            track.enabled = audioEnabled;
        });
        return stream;
    }

    function createPeerConnection() {
        if (peerConnection) {
            return peerConnection;
        }
        peerConnection = new RTCPeerConnection({
            iceServers: [{ urls: 'stun:stun.l.google.com:19302' }]
        });
        peerConnection.onicecandidate = function (event) {
            if (event.candidate && socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({
                    type: 'ice-candidate',
                    payload: event.candidate,
                    targetSessionId: peerConnection.remoteSessionId || null
                }));
            }
        };
        peerConnection.ontrack = function (event) {
            if (event.streams && event.streams[0] && publicRemoteAudio) {
                publicRemoteAudio.srcObject = event.streams[0];
                publicRemoteAudio.muted = false;
                const playPromise = publicRemoteAudio.play();
                if (playPromise && typeof playPromise.catch === 'function') {
                    playPromise.catch(function () {});
                }
                updateVideoStatus('Two-way voice connected');
            }
        };
        return peerConnection;
    }

    async function connectSignal() {
        if (socket && socket.readyState === WebSocket.OPEN) {
            return socket;
        }
        if (signalReadyPromise) {
            return signalReadyPromise;
        }
        const protocol = window.location.protocol === 'https:' ? 'wss://' : 'ws://';
        socket = new WebSocket(protocol + window.location.host + (context.signalUrl || '/signal'));
        signalReadyPromise = new Promise(function (resolve, reject) {
            function clearPending() {
                signalReadyPromise = null;
            }

            function handleOpen() {
                socket.removeEventListener('error', handleError);
                socket.removeEventListener('close', handleCloseBeforeOpen);
                updateVideoStatus('Signal online');
                resolve(socket);
            }

            function handleError() {
                socket.removeEventListener('open', handleOpen);
                socket.removeEventListener('close', handleCloseBeforeOpen);
                clearPending();
                reject(new Error('Unable to connect to the control-room signal channel.'));
            }

            function handleCloseBeforeOpen() {
                socket.removeEventListener('open', handleOpen);
                socket.removeEventListener('error', handleError);
                clearPending();
                reject(new Error('Signal connection closed before video could start.'));
            }

            socket.addEventListener('open', handleOpen, { once: true });
            socket.addEventListener('error', handleError, { once: true });
            socket.addEventListener('close', handleCloseBeforeOpen, { once: true });
        });
        socket.addEventListener('close', function () {
            signalReadyPromise = null;
            socket = null;
            updateVideoStatus('Signal offline');
        });
        socket.addEventListener('message', async function (event) {
            const message = JSON.parse(event.data);
            if (message.type === 'answer' && peerConnection) {
                peerConnection.remoteSessionId = message.senderSessionId || peerConnection.remoteSessionId || null;
                await peerConnection.setRemoteDescription(new RTCSessionDescription(message.payload));
                updateVideoStatus('Control room connected');
            }
            if (message.type === 'ice-candidate' && peerConnection) {
                try {
                    await peerConnection.addIceCandidate(new RTCIceCandidate(message.payload));
                } catch (error) {
                    console.warn(error);
                }
            }
            if (message.type === 'VIDEO_ENDED') {
                if (publicRemoteAudio) {
                    publicRemoteAudio.srcObject = null;
                }
                if (peerConnection) {
                    peerConnection.close();
                    peerConnection = null;
                }
                updateVideoStatus('Camera idle');
            }
        });
        return signalReadyPromise;
    }

    async function startPublicVideo() {
        await connectSignal();
        callerAudioEnabled = !publicAudioMode || String(publicAudioMode.value) !== 'false';
        localStream = await getPreferredMediaStream(callerAudioEnabled);
        syncCallerAudioState();
        if (publicLocalVideo) {
            attachStream(publicLocalVideo, localStream);
        }
        const response = await window.fetch(context.reportVideoStartUrl, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({
                locationLabel: 'Public scene video',
                audioEnabled: String(callerAudioEnabled)
            })
        });
        const payload = await readPayload(response);
        if (!response.ok) {
            throw new Error(payload.error || 'Unable to start video');
        }
        currentVideoSessionId = payload.id;
        createPeerConnection();
        localStream.getTracks().forEach(function (track) {
            peerConnection.addTrack(track, localStream);
        });
        const offer = await peerConnection.createOffer();
        await peerConnection.setLocalDescription(offer);
        socket.send(JSON.stringify({
            type: 'offer',
            payload: offer,
            callId: context.callId,
            audienceStationId: context.stationId,
            audienceType: 'STATION'
        }));
        updateVideoStatus('Broadcasting to control room');
    }

    async function endPublicVideo() {
        if (localStream) {
            stopStream(localStream);
            localStream = null;
            if (publicLocalVideo) {
                publicLocalVideo.srcObject = null;
            }
        }
        if (peerConnection) {
            peerConnection.close();
            peerConnection = null;
        }
        if (currentVideoSessionId) {
            const publicEndUrl = (context.reportVideoEndUrl || '').replace('__id__', String(currentVideoSessionId));
            const response = await window.fetch(publicEndUrl, {
                method: 'POST',
                credentials: 'same-origin'
            });
            const payload = await readPayload(response);
            if (!response.ok) {
                throw new Error(payload.error || 'Unable to end video');
            }
        }
        currentVideoSessionId = null;
        if (publicRemoteAudio) {
            publicRemoteAudio.srcObject = null;
        }
        updateVideoStatus('Camera idle');
    }

    if (publicStartVideo) {
        publicStartVideo.addEventListener('click', function () {
            startPublicVideo().catch(function (error) {
                window.alert(error.message);
            });
        });
    }

    if (publicAudioMode) {
        publicAudioMode.addEventListener('change', function () {
            callerAudioEnabled = String(publicAudioMode.value) !== 'false';
            syncCallerAudioState();
        });
    }

    if (publicToggleAudio) {
        publicToggleAudio.addEventListener('click', function () {
            callerAudioEnabled = !callerAudioEnabled;
            if (publicAudioMode) {
                publicAudioMode.value = callerAudioEnabled ? 'true' : 'false';
            }
            syncCallerAudioState();
        });
        syncCallerAudioState();
    }

    if (publicEndVideo) {
        publicEndVideo.addEventListener('click', function () {
            endPublicVideo().catch(function (error) {
                window.alert(error.message);
            });
        });
    }

    loadMessages().catch(function () {});
    window.setInterval(function () {
        loadMessages().catch(function () {});
    }, 8000);
});
