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

    const body = document.body;
    const currentLang = body ? (body.getAttribute('data-lang') || 'sw').toLowerCase() : 'sw';
    const translations = {
        sw: {
            'Overview': 'Muhtasari',
            'Mission': 'Dhamira',
            'Command': 'Uongozi',
            'Features': 'Vipengele',
            'Public Emergency Access': 'Huduma ya Dharura kwa Umma',
            'Contact': 'Mawasiliano',
            'Login': 'Ingia',
            'Quick Access': 'Ufikiaji wa Haraka',
            'Platform Overview': 'Muhtasari wa Mfumo',
            'Mission Overview': 'Muhtasari wa Dhamira',
            'Command Structure': 'Muundo wa Uongozi',
            'Core Features': 'Vipengele Muhimu',
            'Get Started': 'Anza Hapa',
            'Actions': 'Vitendo',
            'Open Login': 'Fungua Kuingia',
            'Report Emergency': 'Toa Taarifa ya Dharura',
            'Support': 'Msaada',
            'Need command center access?': 'Unahitaji ufikiaji wa kituo cha uongozi?',
            'Operations line:': 'Namba ya operesheni:',
            'Admin email:': 'Barua pepe ya msimamizi:',
            'Government Emergency Operations Platform': 'Mfumo wa Serikali wa Operesheni za Dharura',
            'FROMS delivers a disciplined': 'FROMS inatoa taswira yenye nidhamu',
            'operational picture for fire': 'ya uendeshaji kwa zimamoto',
            'and rescue command.': 'na uongozi wa uokoaji.',
            'FROMS delivers a disciplined operational picture for fire and rescue command.': 'FROMS inatoa taswira thabiti ya uendeshaji kwa uongozi wa zimamoto na uokoaji.',
            'Designed for high-level public service delivery, the platform combines incident intelligence, control room coordination, equipment readiness, and secure field reporting in one modern command environment.': 'Ukiundwa kwa utoaji wa huduma za umma wa kiwango cha juu, mfumo huu unaunganisha taarifa za matukio, uratibu wa chumba cha udhibiti, utayari wa vifaa na utoaji wa taarifa salama kutoka eneo la tukio katika mazingira ya kisasa ya uongozi.',
            'Open Command Access': 'Fungua Ufikiaji wa Uongozi',
            'Operational Modules': 'Moduli za Uendeshaji',
            'National Readiness': 'Utayari wa Kitaifa',
            'Built for headquarters, regional command, district command, station operations, and public intake.': 'Umejengwa kwa makao makuu, uongozi wa mikoa, uongozi wa wilaya, operesheni za vituo na mapokezi ya umma.',
            'Competition Standard': 'Kiwango cha Ushindani',
            'Clean visual hierarchy, strong operational clarity, and audit-aware workflows appropriate for international evaluation.': 'Mpangilio safi wa mwonekano, uwazi mkubwa wa kiutendaji na mtiririko unaozingatia ukaguzi unaofaa kwa tathmini ya kimataifa.',
            'Built for saving lives': 'Umejengwa kuokoa maisha',
            'The system is designed to improve response efficiency, accountability, and operational awareness.': 'Mfumo umeundwa kuboresha ufanisi wa mwitikio, uwajibikaji na uelewa wa uendeshaji.',
            'Incident Visibility': 'Mwonekano wa Matukio',
            'Dispatch Coordination': 'Uratibu wa Upelekaji',
            'Equipment Readiness': 'Utayari wa Vifaa',
            'Equipment Management': 'Usimamizi wa Vifaa',
            'Equipments': 'Vifaa',
            'Decision Support': 'Usaidizi wa Maamuzi',
            'Command Car': 'Gari la Amri',
            'Management Car': 'Gari la Usimamizi',
            'Hazmat Car': 'Gari la Vifaa Hatari',
            'Ambulance': 'Ambulansi',
            'Workspaces from headquarters to station level': 'Maeneo ya kazi kutoka makao makuu hadi ngazi ya kituo',
            'Integrated emergency response modules': 'Moduli zilizounganishwa za mwitikio wa dharura',
            'Open your command workspace or report an emergency.': 'Fungua eneo lako la kazi la uongozi au toa taarifa ya dharura.',
            'Use the login page for officers and command staff, or use the public emergency access page for rapid field reporting.': 'Tumia ukurasa wa kuingia kwa maafisa na watumishi wa uongozi, au tumia ukurasa wa huduma ya dharura kwa umma kwa taarifa za haraka kutoka eneo la tukio.',
            'Emergency Access': 'Ufikiaji wa Dharura',
            'Email': 'Barua Pepe',
            'Home': 'Nyumbani',
            'Operations Line': 'Namba ya Operesheni',
            'Secure Access': 'Ufikiaji Salama',
            'Command Sign-In': 'Kuingia kwa Uongozi',
            'Secure access for national, regional, district, station, and control room operations.': 'Ufikiaji salama kwa operesheni za kitaifa, mkoa, wilaya, kituo na chumba cha udhibiti.',
            'Username': 'Jina la mtumiaji',
            'Password': 'Nenosiri',
            'Signing In...': 'Inaingia...',
            'Command Email': 'Barua Pepe ya Uongozi',
            'Back to landing page': 'Rudi kwenye ukurasa wa mwanzo',
            'Dashboard': 'Dashibodi',
            'Control Room': 'Chumba cha Udhibiti',
            'Dark Mode': 'Mwonekano wa Giza',
            'Profile': 'Wasifu',
            'Change Password': 'Badili Nenosiri',
            'Logout': 'Toka',
            'Public Access': 'Ufikiaji wa Umma',
            'Role Dashboard': 'Dashibodi ya Wajibu',
            'System Test': 'Jaribio la Mfumo',
            'Fire Investigation': 'Uchunguzi wa Moto',
            'Geography': 'Jiografia',
            'Public Emergency Access': 'Huduma ya Dharura kwa Umma',
            'Report Tracking': 'Ufuatiliaji wa Taarifa',
            'Your report has been received': 'Taarifa yako imepokelewa',
            'Use this case room to chat with the assigned control room and, if needed, start a live video view from the scene.': 'Tumia chumba hiki cha taarifa kuwasiliana na chumba cha udhibiti kilichopewa jukumu na, ikihitajika, kuanzisha video ya moja kwa moja kutoka eneo la tukio.',
            'Report Number': 'Namba ya Taarifa',
            'Assigned Station': 'Kituo Kilichopangiwa',
            'Pending': 'Inasubiri',
            'Chat with Control Room': 'Ongea na Chumba cha Udhibiti',
            'Reporter vs station control room': 'Mtoa taarifa dhidi ya chumba cha udhibiti cha kituo',
            'No messages yet.': 'Bado hakuna ujumbe.',
            'Type your message': 'Andika ujumbe wako',
            'Send': 'Tuma',
            'Live Video to Control Room': 'Video ya Moja kwa Moja kwa Chumba cha Udhibiti',
            'Show the control room what is happening on scene': 'Onyesha chumba cha udhibiti kinachoendelea eneo la tukio',
            'Public scene camera': 'Kamera ya umma eneo la tukio',
            'Assigned station control room can watch in real time': 'Chumba cha udhibiti cha kituo kilichopangiwa kinaweza kutazama moja kwa moja',
            'Camera idle': 'Kamera haijaanza',
            'Start Video Call': 'Anza Simu ya Video',
            'End Video': 'Maliza Video',
            'Emergency Report': 'Taarifa ya Dharura',
            'Report a fire, rescue need, or accident': 'Toa taarifa ya moto, hitaji la uokoaji, au ajali',
            'Public reporting channel for citizens, villages, and field witnesses. Every request is validated before it reaches the control room.': 'Njia ya taarifa kwa wananchi, vijiji na mashahidi wa eneo la tukio. Kila ombi linathibitishwa kabla ya kufika chumba cha udhibiti.',
            'Protection': 'Ulinzi',
            'CAPTCHA enabled': 'CAPTCHA imewezeshwa',
            'Rate-limited intake': 'Mapokezi yenye ukomo wa maombi',
            'Caller name': 'Jina la mpigaji',
            'Caller number': 'Namba ya mpigaji',
            'Incident type': 'Aina ya tukio',
            'Select': 'Chagua',
            'Fire': 'Moto',
            'Rescue': 'Uokoaji',
            'Accident': 'Ajali',
            'Region': 'Mkoa',
            'District': 'Wilaya',
            'Nearby fire station': 'Kituo cha zimamoto kilicho karibu',
            'Ward': 'Kata',
            'Village / street': 'Kijiji / mtaa',
            'Nearest landmark': 'Alama ya karibu',
            'Landmark or road': 'Alama au barabara',
            'Incident details': 'Maelezo ya tukio',
            'Explain what is happening now': 'Eleza kinachoendelea sasa',
            'CAPTCHA': 'CAPTCHA',
            'Send Emergency Report': 'Tuma Taarifa ya Dharura',
            'Operations': 'Operesheni',
            'Control Room Dashboard': 'Dashibodi ya Chumba cha Udhibiti',
            'Operations Command Dashboard': 'Dashibodi ya Uongozi wa Operesheni',
            'Signed In User': 'Mtumiaji Aliyeingia',
            'Assigned Role': 'Wajibu Uliopangiwa',
            'Control Room Command': 'Uongozi wa Chumba cha Udhibiti',
            'Incoming calls, active routing, and recording visibility': 'Simu zinazoingia, uelekezaji hai na mwonekano wa rekodi',
            'Centralized view for rapid public intake, station routing, and communication accountability.': 'Mwonekano wa pamoja kwa mapokezi ya haraka ya umma, uelekezaji wa vituo na uwajibikaji wa mawasiliano.',
            'Incoming Calls': 'Simu Zinazoingia',
            'Active Calls': 'Simu Hai',
            'Recordings Available': 'Rekodi Zilizopo',
            'Live Streams': 'Mitiririko ya Moja kwa Moja',
            'Live Video Command Panel': 'Paneli ya Uongozi wa Video Mubashara',
            'Signal Controls': 'Udhibiti wa Mawimbi',
            'Start Live Video': 'Anza Video Mubashara',
            'End Stream': 'Maliza Mtiririko',
            'Recording Upload': 'Pakia Rekodi',
            'Live Session Feed': 'Mtiririko wa Vipindi Mubashara',
            'Call History': 'Historia ya Simu',
            'Caller': 'Mpigaji',
            'Station': 'Kituo',
            'Recording': 'Rekodi',
            'Chat': 'Mazungumzo',
            'Open': 'Fungua',
            'Incident Map': 'Ramani ya Tukio',
            'Notification Feed': 'Mtiririko wa Taarifa',
            'Session Metrics': 'Vipimo vya Kipindi',
            'Total Sessions': 'Jumla ya Vipindi',
            'Recorded Streams': 'Mitiririko Iliyorekodiwa',
            'National Command View': 'Mwonekano wa Uongozi wa Kitaifa',
            'Operational picture for fire and rescue leadership': 'Taswira ya uendeshaji kwa uongozi wa zimamoto na uokoaji',
            'Monitor incidents, field activity, equipment posture, and command recommendations through one government-grade dashboard.': 'Fuatilia matukio, shughuli za uwanjani, hali ya vifaa na mapendekezo ya uongozi kupitia dashibodi moja ya kiwango cha serikali.',
            'Total Incidents': 'Jumla ya Matukio',
            'Active Emergencies': 'Dharura Hai',
            'Average Response Time': 'Wastani wa Muda wa Mwitikio',
            'Maintenance Alerts': 'Tahadhari za Matengenezo',
            'Incident Feed': 'Mtiririko wa Matukio',
            'High-Risk Areas': 'Maeneo Yenye Hatari Kubwa',
            'AI Recommendations': 'Mapendekezo ya AI',
            'Resource Snapshot': 'Muhtasari wa Rasilimali',
            'Live Map Panel': 'Paneli ya Ramani Hai',
            'System Verification': 'Uthibitishaji wa Mfumo',
            'Start System Testing': 'Anza Upimaji wa Mfumo',
            'System Verification Report': 'Ripoti ya Uthibitishaji wa Mfumo',
            'Reports': 'Ripoti',
            'Notifications': 'Arifa',
            'Analytics': 'Uchanganuzi',
            'Tele Rescue': 'Tele Uokoaji',
            'Report Center': 'Kituo cha Ripoti',
            'System Documentation': 'Nyaraka za Mfumo',
            'Notification Brief': 'Muhtasari wa Arifa',
            'EN PDF': 'PDF EN',
            'SW PDF': 'PDF SW',
            'View': 'Tazama',
            'Download PDF': 'Pakua PDF',
            'Create User': 'Unda Mtumiaji',
            'Total Users': 'Jumla ya Watumiaji',
            'Inactive Users': 'Watumiaji Wasio Hai',
            'Weak Passwords': 'Nywila Dhaifu',
            'Update Password': 'Sasisha Nenosiri',
            'Cancel': 'Ghairi'
        }
    };

    function translateTextContent(root, dictionary) {
        if (!root || !dictionary) {
            return;
        }
        const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
        const nodes = [];
        while (walker.nextNode()) {
            nodes.push(walker.currentNode);
        }
        nodes.forEach(function (node) {
            const original = node.nodeValue;
            const trimmed = original ? original.trim() : '';
            if (!trimmed || !dictionary[trimmed]) {
                return;
            }
            const leading = original.match(/^\s*/);
            const trailing = original.match(/\s*$/);
            node.nodeValue = (leading ? leading[0] : '') + dictionary[trimmed] + (trailing ? trailing[0] : '');
        });

        root.querySelectorAll('input[placeholder], textarea[placeholder]').forEach(function (element) {
            const placeholder = element.getAttribute('placeholder');
            if (placeholder && dictionary[placeholder.trim()]) {
                element.setAttribute('placeholder', dictionary[placeholder.trim()]);
            }
        });

        root.querySelectorAll('option').forEach(function (option) {
            const text = option.textContent ? option.textContent.trim() : '';
            if (text && dictionary[text]) {
                option.textContent = dictionary[text];
            }
        });
    }

    if (currentLang === 'sw' && translations.sw) {
        translateTextContent(document.body, translations.sw);
        if (document.title && translations.sw[document.title]) {
            document.title = translations.sw[document.title];
        }
    }

    function renderDashboardChart(canvasId, type, labels, values, colors, options) {
        if (typeof Chart === 'undefined') {
            return;
        }
        const canvas = document.getElementById(canvasId);
        if (!canvas || !Array.isArray(labels) || !Array.isArray(values)) {
            return;
        }
        const context = canvas.getContext('2d');
        if (!context) {
            return;
        }
        new Chart(context, {
            type: type,
            data: {
                labels: labels,
                datasets: [{
                    data: values,
                    backgroundColor: colors,
                    borderColor: '#7c3c00',
                    borderWidth: 1.6,
                    borderRadius: type === 'bar' ? 14 : 0,
                    maxBarThickness: 42
                }]
            },
            options: Object.assign({
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            usePointStyle: true,
                            boxWidth: 10,
                            color: '#5f2400',
                            font: {
                                family: 'Public Sans, Inter, system-ui',
                                size: 12,
                                weight: '600'
                            }
                        }
                    },
                    animation: {
                        duration: 900,
                        easing: 'easeOutCubic'
                    }
                },
                scales: type === 'bar' ? {
                    x: {
                        grid: { display: false },
                        ticks: { color: '#7c3c00', font: { family: 'Public Sans, Inter, system-ui' } }
                    },
                    y: {
                        beginAtZero: true,
                        grid: { color: 'rgba(245, 124, 0, 0.14)' },
                        ticks: {
                            precision: 0,
                            color: '#7c3c00',
                            font: { family: 'Public Sans, Inter, system-ui' }
                        }
                    }
                } : {}
            }, options || {})
        });
    }

    function normalizeChartSeries(labels, values, emptyLabel) {
        const safeLabels = Array.isArray(labels) ? labels.map(function (label) {
            return String(label);
        }) : [];
        const safeValues = Array.isArray(values) ? values.map(function (value) {
            const numeric = Number(value);
            return Number.isFinite(numeric) ? numeric : 0;
        }) : [];
        const total = safeValues.reduce(function (sum, value) {
            return sum + value;
        }, 0);
        if (safeLabels.length > 0 && safeLabels.length === safeValues.length && total > 0) {
            return { labels: safeLabels, values: safeValues };
        }
        return {
            labels: [emptyLabel],
            values: [1]
        };
    }

    function initDashboardCharts() {
        const roleCharts = window.FROMS_ROLE_CHARTS;
        if (roleCharts) {
            const incidentMix = normalizeChartSeries(roleCharts.incidentLabels, roleCharts.incidentValues, 'No incidents in scope');
            const supportMix = normalizeChartSeries(roleCharts.supportLabels, roleCharts.supportValues, 'No support load recorded');
            renderDashboardChart('roleIncidentMixChart', 'doughnut', incidentMix.labels, incidentMix.values, ['#f97316', '#7c2d12', '#0f766e']);
            renderDashboardChart('roleSupportChart', 'bar', supportMix.labels, supportMix.values, ['#f97316', '#f59e0b', '#0f766e']);
            renderDashboardChart('roleResourceChart', 'bar', roleCharts.resourceLabels || ['Available', 'In Use', 'Maintenance'], roleCharts.resourceValues || [0, 0, 0], ['#0f766e', '#f97316', '#b91c1c']);
        }

        const operationsCharts = window.FROMS_OPERATIONS_CHARTS;
        if (operationsCharts) {
            const incidentSeries = normalizeChartSeries(['Active emergencies', 'Stabilized incidents'], operationsCharts.incidentValues, 'No incidents recorded');
            const riskSeries = normalizeChartSeries(operationsCharts.riskLabels, operationsCharts.riskValues, 'No mapped risk hotspots');
            renderDashboardChart('operationsIncidentChart', 'doughnut', incidentSeries.labels, incidentSeries.values, ['#f97316', '#7c2d12']);
            renderDashboardChart('operationsRiskChart', 'bar', riskSeries.labels, riskSeries.values, ['#b91c1c', '#f97316', '#f59e0b', '#fb923c', '#0f766e']);
            renderDashboardChart('operationsResourceChart', 'bar', ['Available', 'Deployed', 'Maintenance'], operationsCharts.resourceValues || [0, 0, 0], ['#0f766e', '#f59e0b', '#b91c1c']);
            renderDashboardChart('operationsPerformanceChart', 'bar', ['Active emergencies', 'Avg response (min)', 'Stations', 'AI recommendations'], operationsCharts.performanceValues || [0, 0, 0, 0], ['#f97316', '#c2410c', '#0f766e', '#7c2d12']);
        }

        const controlRoomCharts = window.FROMS_CONTROL_ROOM_CHARTS;
        if (controlRoomCharts) {
            renderDashboardChart('controlRoomCallChart', 'bar', ['Incoming', 'Active', 'Recorded'], controlRoomCharts.callValues || [0, 0, 0], ['#f97316', '#fb923c', '#0f766e']);
            renderDashboardChart('controlRoomVideoChart', 'doughnut', ['Live streams', 'Recorded streams', 'Total sessions'], controlRoomCharts.videoValues || [0, 0, 0], ['#b91c1c', '#f59e0b', '#7c2d12']);
        }
    }

    initDashboardCharts();

    function createOptionElement(value, label) {
        const option = document.createElement('option');
        option.value = value;
        option.textContent = label;
        return option;
    }

    function resetSelectOptions(selectElement, placeholder) {
        if (!selectElement) {
            return;
        }
        selectElement.replaceChildren(createOptionElement('', placeholder));
    }

    function createEmptyStateNode(text) {
        const node = document.createElement('div');
        node.className = 'small ui-muted';
        node.textContent = text;
        return node;
    }

    function createListRowElement(primaryText, secondaryText, trailingText) {
        const row = document.createElement('div');
        row.className = 'ui-list-row';

        const content = document.createElement('div');
        const title = document.createElement('div');
        title.className = 'fw-semibold';
        title.textContent = primaryText;
        content.appendChild(title);

        if (secondaryText) {
            const subtitle = document.createElement('div');
            subtitle.className = 'small ui-muted';
            subtitle.textContent = secondaryText;
            content.appendChild(subtitle);
        }

        const trailing = document.createElement('strong');
        trailing.textContent = trailingText;

        row.appendChild(content);
        row.appendChild(trailing);
        return row;
    }

    function createStatusFeedRow(title, status) {
        const row = document.createElement('div');
        row.className = 'ui-list-row';
        const titleNode = document.createElement('span');
        titleNode.textContent = title;
        const statusNode = document.createElement('strong');
        statusNode.textContent = status;
        row.appendChild(titleNode);
        row.appendChild(statusNode);
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
        content.appendChild(linePrimary);
        content.appendChild(lineSecondary);

        const trailing = document.createElement('div');
        trailing.className = 'ui-skeleton-block ui-skeleton-block-sm';

        row.appendChild(content);
        row.appendChild(trailing);
        return row;
    }

    function renderLoadingSkeleton(container, count) {
        if (!container) {
            return;
        }
        const fragment = document.createDocumentFragment();
        for (let index = 0; index < count; index += 1) {
            fragment.appendChild(createSkeletonRow());
        }
        container.replaceChildren(fragment);
    }

    function renderListRows(container, items, emptyMessage, mapper) {
        if (!container) {
            return;
        }
        const fragment = document.createDocumentFragment();
        if (!Array.isArray(items) || items.length === 0) {
            fragment.appendChild(createEmptyStateNode(emptyMessage));
        } else {
            items.forEach(function (item) {
                fragment.appendChild(mapper(item));
            });
        }
        container.replaceChildren(fragment);
    }

    const protectedPrefixes = ['/operations', '/control-room', '/dashboard'];
    const currentPath = window.location.pathname || '';
    const isProtectedPage = protectedPrefixes.some(function (prefix) {
        return currentPath === prefix || currentPath.startsWith(prefix + '/') || currentPath.startsWith(prefix + '?');
    });
    const appContext = window.FROMS_CONTEXT || {};
    function normalizeRole(roleValue) {
        if (!roleValue) {
            return '';
        }
        const normalized = String(roleValue).trim().toUpperCase();
        return normalized.startsWith('ROLE_') ? normalized.substring(5) : normalized;
    }
    const role = normalizeRole(body ? (body.getAttribute('data-role') || appContext.role || '') : '');
    const canPublish = Boolean(appContext.canStartLiveVideo)
        || role === 'STATION_OPERATION_OFFICER'
        || role === 'CONTROL_ROOM_ATTENDANT';
    const isControlRoomPage = currentPath === '/control-room/dashboard' || currentPath.startsWith('/control-room/');
    const teleSupportRoles = ['TELE_SUPPORT_PERSONNEL', 'HEAD_FIRE_FIGHTING_OPERATIONS', 'HEAD_RESCUE_OPERATIONS'];
    const nationalScopeRoles = ['SUPER_ADMIN', 'CGF', 'COMMISSIONER_OPERATIONS', 'HEAD_FIRE_FIGHTING_OPERATIONS', 'HEAD_RESCUE_OPERATIONS', 'CHIEF_FIRE_OFFICER', 'FIRE_INVESTIGATION_HOD'];
    const dashboardWorkbench = document.querySelector('.dashboard-workbench');
    const sidebarToggleButtons = document.querySelectorAll('[data-sidebar-toggle]');

    function matchesCurrentStation(stationId) {
        if (stationId === null || typeof stationId === 'undefined' || stationId === '') {
            return false;
        }
        return String(appContext.stationId || '') === String(stationId);
    }

    function matchesCurrentDistrict(districtId) {
        if (districtId === null || typeof districtId === 'undefined' || districtId === '') {
            return false;
        }
        return String(appContext.districtId || '') === String(districtId);
    }

    function matchesCurrentRegion(regionId) {
        if (regionId === null || typeof regionId === 'undefined' || regionId === '') {
            return false;
        }
        return String(appContext.regionId || '') === String(regionId);
    }

    function isTeleSupportSpecialistRole() {
        return teleSupportRoles.indexOf(role) >= 0;
    }

    function isNationalScopeRole() {
        return nationalScopeRoles.indexOf(role) >= 0;
    }

    function compactText(value) {
        if (value === null || typeof value === 'undefined') {
            return '';
        }
        return String(value).trim();
    }

    function joinMeta(parts) {
        return parts.map(compactText).filter(Boolean).join(' | ');
    }

    function matchesScope(payload) {
        if (!payload) {
            return false;
        }
        if (isNationalScopeRole()) {
            return true;
        }
        if (isControlRoomPage && (role === 'CONTROL_ROOM_ATTENDANT' || role === 'CONTROL_ROOM_OPERATOR')) {
            return true;
        }
        if (payload.stationId !== null && typeof payload.stationId !== 'undefined' && payload.stationId !== '') {
            return matchesCurrentStation(payload.stationId);
        }
        if (appContext.stationId) {
            return false;
        }
        if (matchesCurrentDistrict(payload.districtId)) {
            return true;
        }
        if (appContext.districtId) {
            return false;
        }
        return matchesCurrentRegion(payload.regionId);
    }

    function shouldAutoAnswerOffer(message) {
        if (message.audienceStationId) {
            return matchesCurrentStation(message.audienceStationId);
        }
        if (isTeleSupportSpecialistRole()) {
            return message.incidentId !== null
                && typeof message.incidentId !== 'undefined'
                && pendingTeleSupportIncidentId !== null
                && String(pendingTeleSupportIncidentId) === String(message.incidentId);
        }
        return true;
    }

    function syncSidebarToggleButtons(collapsed) {
        sidebarToggleButtons.forEach(function (button) {
            button.textContent = collapsed ? 'Expand Sidebar' : 'Collapse Sidebar';
            button.setAttribute('aria-pressed', collapsed ? 'true' : 'false');
        });
    }

    function applySidebarState(collapsed) {
        if (!dashboardWorkbench) {
            return;
        }
        dashboardWorkbench.classList.toggle('dashboard-sidebar-collapsed', collapsed);
        syncSidebarToggleButtons(collapsed);
    }

    if (dashboardWorkbench && sidebarToggleButtons.length > 0) {
        const sidebarPreferenceKey = 'froms:sidebar:' + (body ? (body.getAttribute('data-page') || 'default') : 'default');
        applySidebarState(window.localStorage.getItem(sidebarPreferenceKey) === 'collapsed');
        sidebarToggleButtons.forEach(function (button) {
            button.addEventListener('click', function () {
                const collapsed = !dashboardWorkbench.classList.contains('dashboard-sidebar-collapsed');
                applySidebarState(collapsed);
                window.localStorage.setItem(sidebarPreferenceKey, collapsed ? 'collapsed' : 'expanded');
            });
        });
    }

    document.querySelectorAll('[data-history-back]').forEach(function (button) {
        button.addEventListener('click', function () {
            if (window.history.length > 1) {
                window.history.back();
                return;
            }
            window.location.href = '/';
        });
    });

    document.querySelectorAll('[data-rotator]').forEach(function (rotator) {
        const slides = Array.from(rotator.querySelectorAll('[data-rotator-slide]'));
        const dots = Array.from(rotator.querySelectorAll('[data-rotator-dot]'));
        const delay = Number(rotator.getAttribute('data-rotator-delay') || '4000');
        if (slides.length === 0) {
            return;
        }

        let index = 0;
        let timerId = null;

        function render(nextIndex) {
            index = (nextIndex + slides.length) % slides.length;
            slides.forEach(function (slide, slideIndex) {
                const active = slideIndex === index;
                slide.classList.toggle('is-active', active);
                slide.setAttribute('aria-hidden', active ? 'false' : 'true');
            });
            dots.forEach(function (dot, dotIndex) {
                dot.classList.toggle('is-active', dotIndex === index);
            });
        }

        function start() {
            if (slides.length < 2) {
                return;
            }
            stop();
            timerId = window.setInterval(function () {
                render(index + 1);
            }, delay);
        }

        function stop() {
            if (timerId !== null) {
                window.clearInterval(timerId);
                timerId = null;
            }
        }

        dots.forEach(function (dot, dotIndex) {
            dot.addEventListener('click', function () {
                render(dotIndex);
                start();
            });
        });

        rotator.addEventListener('mouseenter', stop);
        rotator.addEventListener('mouseleave', start);

        render(0);
        start();
    });

    const publicGeoContext = window.FROMS_PUBLIC_GEO || {};
    if (publicGeoContext.geographyApiUrl) {
        const publicRegionId = document.getElementById('publicRegionId');
        const publicDistrictId = document.getElementById('publicDistrictId');
        const publicStationId = document.getElementById('publicStationId');
        const publicWardId = document.getElementById('ward');
        const publicVillageStreetId = document.getElementById('village');
        const publicRoadLandmarkId = document.getElementById('locationText');

        function showUiError(message) {
            if (typeof Swal !== 'undefined') {
                Swal.fire({ icon: 'error', title: 'Request failed', text: message });
                return;
            }
            window.alert(message);
        }

        async function fetchPublicOptions(url, selectElement, placeholder) {
            if (!selectElement) {
                return;
            }
            resetSelectOptions(selectElement, placeholder);
            selectElement.disabled = true;
            const response = await window.fetch(url, { method: 'GET', credentials: 'same-origin' });
            const payload = await response.json();
            if (!response.ok) {
                throw new Error(payload.error || 'Unable to load options');
            }
            payload.forEach(function (item) {
                const option = document.createElement('option');
                option.value = String(item.id);
                option.textContent = item.name;
                selectElement.appendChild(option);
            });
            selectElement.disabled = false;
        }

        if (publicRegionId && publicDistrictId) {
            publicRegionId.addEventListener('change', function () {
                if (publicStationId) {
                    resetSelectOptions(publicStationId, 'Select station');
                    publicStationId.disabled = true;
                }
                if (publicWardId) {
                    resetSelectOptions(publicWardId, 'Select ward');
                    publicWardId.disabled = true;
                }
                if (publicVillageStreetId) {
                    resetSelectOptions(publicVillageStreetId, 'Select village or street');
                    publicVillageStreetId.disabled = true;
                }
                if (publicRoadLandmarkId) {
                    resetSelectOptions(publicRoadLandmarkId, 'Select road landmark');
                    publicRoadLandmarkId.disabled = true;
                }
                if (!publicRegionId.value) {
                    resetSelectOptions(publicDistrictId, 'Select district');
                    publicDistrictId.disabled = true;
                    return;
                }
                fetchPublicOptions(publicGeoContext.geographyApiUrl + '/regions/' + publicRegionId.value + '/districts', publicDistrictId, 'Select district').catch(function (error) {
                    showUiError(error.message);
                });
            });
        }

        if (publicDistrictId && publicStationId) {
            publicDistrictId.addEventListener('change', function () {
                if (publicWardId) {
                    resetSelectOptions(publicWardId, 'Select ward');
                    publicWardId.disabled = true;
                }
                if (publicVillageStreetId) {
                    resetSelectOptions(publicVillageStreetId, 'Select village or street');
                    publicVillageStreetId.disabled = true;
                }
                if (publicRoadLandmarkId) {
                    resetSelectOptions(publicRoadLandmarkId, 'Select road landmark');
                    publicRoadLandmarkId.disabled = true;
                }
                if (!publicDistrictId.value) {
                    resetSelectOptions(publicStationId, 'Select station');
                    publicStationId.disabled = true;
                    return;
                }
                fetchPublicOptions(publicGeoContext.geographyApiUrl + '/districts/' + publicDistrictId.value + '/stations', publicStationId, 'Select station').catch(function (error) {
                    showUiError(error.message);
                });
                fetchPublicOptions(publicGeoContext.geographyApiUrl + '/districts/' + publicDistrictId.value + '/wards', publicWardId, 'Select ward').catch(function (error) {
                    showUiError(error.message);
                });
            });
        }

        if (publicWardId && publicVillageStreetId) {
            publicWardId.addEventListener('change', function () {
                if (publicRoadLandmarkId) {
                    resetSelectOptions(publicRoadLandmarkId, 'Select road landmark');
                    publicRoadLandmarkId.disabled = true;
                }
                if (!publicWardId.value) {
                    resetSelectOptions(publicVillageStreetId, 'Select village or street');
                    publicVillageStreetId.disabled = true;
                    return;
                }
                fetchPublicOptions(publicGeoContext.geographyApiUrl + '/wards/' + publicWardId.value + '/villages-streets', publicVillageStreetId, 'Select village or street').catch(function (error) {
                    showUiError(error.message);
                });
            });
        }

        if (publicVillageStreetId && publicRoadLandmarkId) {
            publicVillageStreetId.addEventListener('change', function () {
                if (!publicVillageStreetId.value) {
                    resetSelectOptions(publicRoadLandmarkId, 'Select road landmark');
                    publicRoadLandmarkId.disabled = true;
                    return;
                }
                fetchPublicOptions(publicGeoContext.geographyApiUrl + '/villages-streets/' + publicVillageStreetId.value + '/road-landmarks', publicRoadLandmarkId, 'Select road landmark').catch(function (error) {
                    showUiError(error.message);
                });
            });
        }
    }

    function showInlineAlertToasts() {
        if (typeof Swal === 'undefined') {
            return;
        }
        document.querySelectorAll('.ui-alert[data-alert-kind]').forEach(function (alertNode) {
            if (alertNode.dataset.toastShown === 'true') {
                return;
            }
            alertNode.dataset.toastShown = 'true';
            Swal.fire({
                toast: true,
                position: 'top-end',
                icon: alertNode.dataset.alertKind || 'info',
                title: alertNode.textContent.trim(),
                showConfirmButton: false,
                timer: 4200,
                timerProgressBar: true
            });
        });
    }

    showInlineAlertToasts();

    const publicEmergencyForm = document.getElementById('publicEmergencyForm');
    const publicEmergencySubmit = document.getElementById('publicEmergencySubmit');
    if (publicEmergencyForm) {
        publicEmergencyForm.addEventListener('submit', function (event) {
            if (!publicEmergencyForm.checkValidity()) {
                event.preventDefault();
                publicEmergencyForm.classList.add('was-validated');
                return;
            }
            if (publicEmergencyForm.dataset.confirmed === 'true') {
                if (publicEmergencySubmit) {
                    publicEmergencySubmit.classList.add('btn-loading');
                    publicEmergencySubmit.setAttribute('disabled', 'disabled');
                }
                return;
            }
            event.preventDefault();
            confirmActionPrompt('Send emergency report?', 'This report will be submitted to the control room for verification and routing.', 'Send report').then(function (confirmed) {
                if (!confirmed) {
                    return;
                }
                publicEmergencyForm.dataset.confirmed = 'true';
                if (publicEmergencySubmit) {
                    publicEmergencySubmit.classList.add('btn-loading');
                    publicEmergencySubmit.setAttribute('disabled', 'disabled');
                }
                publicEmergencyForm.submit();
            });
        });
    }

    if (!isProtectedPage || typeof Swal === 'undefined') {
        return;
    }

    const warningAfterMs = 5 * 60 * 1000;
    const responseWindowMs = 50 * 1000;
    const activityEvents = ['click', 'keydown', 'mousemove', 'scroll', 'touchstart'];
    let inactivityTimerId = null;
    let warningOpen = false;

    async function readResponsePayload(response) {
        const text = await response.text();
        if (!text) {
            return {};
        }
        try {
            return JSON.parse(text);
        } catch (error) {
            return { message: text };
        }
    }

    function scheduleWarning() {
        if (inactivityTimerId !== null) {
            window.clearTimeout(inactivityTimerId);
        }
        if (warningOpen) {
            return;
        }
        inactivityTimerId = window.setTimeout(showSessionWarning, warningAfterMs);
    }

    async function keepSessionAlive() {
        const response = await window.fetch('/api/session/keepalive', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });
        if (!response.ok) {
            throw new Error('Session keepalive failed');
        }
    }

    function expireSession() {
        window.location.href = '/logout?reason=expired';
    }

    function showSessionWarning() {
        warningOpen = true;
        Swal.fire({
            title: 'Session Expired',
            text: 'Your session expired after 5 minutes of inactivity. Do you want to continue?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, continue',
            cancelButtonText: 'No, logout',
            confirmButtonColor: '#f57c00',
            cancelButtonColor: '#f57c00',
            timer: responseWindowMs,
            timerProgressBar: true,
            allowOutsideClick: false,
            allowEscapeKey: false
        }).then(function (result) {
            warningOpen = false;
            if (result.isConfirmed) {
                keepSessionAlive()
                    .then(function () {
                        scheduleWarning();
                    })
                    .catch(function () {
                        expireSession();
                    });
                return;
            }
            expireSession();
        });
    }

    activityEvents.forEach(function (eventName) {
        window.addEventListener(eventName, function () {
            if (!warningOpen) {
                scheduleWarning();
            }
        }, { passive: true });
    });

    scheduleWarning();

    const darkModeToggle = document.querySelector('[data-dark-mode-toggle]');
    const darkModeKey = 'froms-dark-mode';

    function setDarkMode(enabled) {
        if (!body) {
            return;
        }
        body.classList.toggle('dark-mode', enabled);
        window.localStorage.setItem(darkModeKey, enabled ? '1' : '0');
    }

    if (window.localStorage.getItem(darkModeKey) === '1') {
        setDarkMode(true);
    }

    if (darkModeToggle) {
        darkModeToggle.addEventListener('click', function (event) {
            event.preventDefault();
            setDarkMode(!body.classList.contains('dark-mode'));
        });
    }

    const changePasswordForm = document.getElementById('changePasswordForm');
    if (changePasswordForm && appContext.changePasswordUrl) {
        changePasswordForm.addEventListener('submit', function (event) {
            event.preventDefault();
            const currentPassword = changePasswordForm.querySelector('#currentPassword').value;
            const newPassword = changePasswordForm.querySelector('#newPassword').value;
            const confirmPassword = changePasswordForm.querySelector('#confirmPassword').value;

            if (newPassword !== confirmPassword) {
                Swal.fire({ icon: 'error', title: 'Password mismatch', text: 'The new password and confirmation do not match.' });
                return;
            }

            confirmActionPrompt('Update password?', 'This will replace your current password for future sign-ins.', 'Update password').then(function (confirmed) {
                if (!confirmed) {
                    return null;
                }
                return window.fetch(appContext.changePasswordUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ currentPassword: currentPassword, newPassword: newPassword })
                }).then(async function (response) {
                    const payload = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payload.error || payload.message || 'Unable to update password');
                    }
                    const passwordModalElement = document.getElementById('changePasswordModal');
                    const passwordModal = passwordModalElement ? bootstrap.Modal.getInstance(passwordModalElement) : null;
                    if (passwordModal) {
                        passwordModal.hide();
                    }
                    changePasswordForm.reset();
                    Swal.fire({ icon: 'success', title: 'Password updated', text: payload.message || 'Your password has been updated.' });
                });
            }).catch(function (error) {
                if (error) {
                    Swal.fire({ icon: 'error', title: 'Password update failed', text: error.message });
                }
            });
        });
    }

    if (appContext.canManageUsers && appContext.usersApiUrl) {
        const nationalRoles = new Set([
            'SUPER_ADMIN',
            'CGF',
            'COMMISSIONER_OPERATIONS',
            'HEAD_FIRE_FIGHTING_OPERATIONS',
            'HEAD_RESCUE_OPERATIONS',
            'FIRE_INVESTIGATION_HOD'
        ]);
        const userFormModal = document.getElementById('userFormModal');
        const userManagementForm = document.getElementById('userManagementForm');
        const skipLegacyUserForm = userManagementForm && userManagementForm.dataset.skipLegacyHandler === 'true';
        const managedUserId = document.getElementById('managedUserId');
        const managedUsername = document.getElementById('managedUsername');
        const managedFullName = document.getElementById('managedFullName');
        const managedDesignation = document.getElementById('managedDesignation');
        const managedPhoneNumber = document.getElementById('managedPhoneNumber');
        const managedRole = document.getElementById('managedRole');
        const managedStationId = document.getElementById('managedStationId');
        const managedPassword = document.getElementById('managedPassword');
        const userFormTitle = document.getElementById('userFormTitle');

        function populateRoleOptions() {
            managedRole.replaceChildren();
            (appContext.roles || []).forEach(function (roleOption) {
                const option = document.createElement('option');
                option.value = roleOption;
                option.textContent = roleOption.replaceAll('_', ' ');
                managedRole.appendChild(option);
            });
        }

        function populateStationOptions() {
            resetSelectOptions(managedStationId, 'Select station');
            (appContext.stations || []).forEach(function (station) {
                const option = document.createElement('option');
                option.value = String(station.id);
                option.textContent = station.name || ('Station ' + station.id);
                managedStationId.appendChild(option);
            });
        }

        function syncStationField() {
            const isNationalRole = nationalRoles.has(managedRole.value);
            managedStationId.disabled = isNationalRole;
            if (isNationalRole) {
                managedStationId.value = '';
            }
        }

        function resetUserForm() {
            managedUserId.value = '';
            managedUsername.value = '';
            managedFullName.value = '';
            managedDesignation.value = '';
            managedPhoneNumber.value = '';
            managedPassword.value = '';
            populateRoleOptions();
            populateStationOptions();
            syncStationField();
            userFormTitle.textContent = 'Create User';
            managedPassword.required = true;
        }

        if (managedRole && !skipLegacyUserForm) {
            managedRole.addEventListener('change', syncStationField);
        }

        if (userFormModal && !skipLegacyUserForm) {
            userFormModal.addEventListener('show.bs.modal', function (event) {
                resetUserForm();
                const trigger = event.relatedTarget;
                if (!trigger || trigger.getAttribute('data-user-action') !== 'edit') {
                    return;
                }
                userFormTitle.textContent = 'Edit User';
                managedPassword.required = false;
                managedUserId.value = trigger.getAttribute('data-user-id') || '';
                managedUsername.value = trigger.getAttribute('data-user-username') || '';
                managedFullName.value = trigger.getAttribute('data-user-full-name') || '';
                managedDesignation.value = trigger.getAttribute('data-user-designation') || '';
                managedPhoneNumber.value = trigger.getAttribute('data-user-phone') || '';
                managedRole.value = trigger.getAttribute('data-user-role') || '';
                managedStationId.value = trigger.getAttribute('data-user-station-id') || '';
                syncStationField();
            });
        }

        if (userManagementForm && !skipLegacyUserForm) {
            userManagementForm.addEventListener('submit', function (event) {
                event.preventDefault();
                const userId = managedUserId.value;
                const stationId = managedStationId.disabled || !managedStationId.value ? '' : managedStationId.value;
                const query = stationId ? ('?stationId=' + encodeURIComponent(stationId)) : '';
                const method = userId ? 'PUT' : 'POST';
                const url = appContext.usersApiUrl + (userId ? '/' + userId : '') + query;
                const payload = {
                    username: managedUsername.value,
                    fullName: managedFullName.value,
                    designation: managedDesignation.value,
                    phoneNumber: managedPhoneNumber.value,
                    role: managedRole.value
                };
                if (managedPassword.value) {
                    payload.password = managedPassword.value;
                }

                confirmActionPrompt(userId ? 'Update user account?' : 'Create user account?', 'This user record will be saved with the selected role and station scope.', userId ? 'Update user' : 'Create user').then(function (confirmed) {
                    if (!confirmed) {
                        return null;
                    }
                    return window.fetch(url, {
                        method: method,
                        credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify(payload)
                    }).then(async function (response) {
                        const responsePayload = await readResponsePayload(response);
                        if (!response.ok) {
                            throw new Error(responsePayload.error || responsePayload.message || 'Unable to save user');
                        }
                        window.location.reload();
                    });
                }).catch(function (error) {
                    if (error) {
                        Swal.fire({ icon: 'error', title: 'User save failed', text: error.message });
                    }
                });
            });
        }

        document.querySelectorAll('[data-lock-user]').forEach(function (button) {
            button.addEventListener('click', function () {
                const userId = button.getAttribute('data-lock-user');
                Swal.fire({
                    icon: 'warning',
                    title: 'Lock account',
                    text: 'This account will be blocked until an administrator unlocks it.',
                    showCancelButton: true,
                    confirmButtonText: 'Lock account'
                }).then(function (result) {
                    if (!result.isConfirmed) {
                        return;
                    }
                    window.fetch(appContext.usersApiUrl + '/' + userId + '/lock', {
                        method: 'POST',
                        credentials: 'same-origin'
                    }).then(function (response) {
                        if (!response.ok) {
                            throw new Error('Unable to lock user');
                        }
                        window.location.reload();
                    }).catch(function (error) {
                        Swal.fire({ icon: 'error', title: 'Lock failed', text: error.message });
                    });
                });
            });
        });

        document.querySelectorAll('[data-unlock-user]').forEach(function (button) {
            button.addEventListener('click', function () {
                const userId = button.getAttribute('data-unlock-user');
                window.fetch(appContext.usersApiUrl + '/' + userId + '/unlock', {
                    method: 'POST',
                    credentials: 'same-origin'
                }).then(function (response) {
                    if (!response.ok) {
                        throw new Error('Unable to unlock user');
                    }
                    window.location.reload();
                }).catch(function (error) {
                    Swal.fire({ icon: 'error', title: 'Unlock failed', text: error.message });
                });
            });
        });

        document.querySelectorAll('[data-delete-user]').forEach(function (button) {
            button.addEventListener('click', function () {
                const userId = button.getAttribute('data-delete-user');
                Swal.fire({
                    icon: 'warning',
                    title: 'Delete account',
                    text: 'This permanently removes the user account.',
                    showCancelButton: true,
                    confirmButtonText: 'Delete user'
                }).then(function (result) {
                    if (!result.isConfirmed) {
                        return;
                    }
                    window.fetch(appContext.usersApiUrl + '/' + userId, {
                        method: 'DELETE',
                        credentials: 'same-origin'
                    }).then(function (response) {
                        if (!response.ok) {
                            throw new Error('Unable to delete user');
                        }
                        window.location.reload();
                    }).catch(function (error) {
                        Swal.fire({ icon: 'error', title: 'Delete failed', text: error.message });
                    });
                });
            });
        });
    }

    if (appContext.canAccessInvestigations && appContext.investigationApiUrl) {
        const investigationForm = document.getElementById('investigationForm');
        if (investigationForm) {
            investigationForm.addEventListener('submit', function (event) {
                event.preventDefault();
                const formData = new FormData();
                formData.append('incidentId', document.getElementById('investigationIncidentId').value);
                formData.append('incidentDetails', document.getElementById('investigationDetails').value);
                formData.append('causeAnalysis', document.getElementById('investigationCauseAnalysis').value);
                formData.append('witnessStatements', document.getElementById('investigationWitnessStatements').value);
                const files = document.getElementById('investigationAttachments').files;
                Array.from(files).forEach(function (file) {
                    formData.append('attachments', file);
                });

                window.fetch(appContext.investigationApiUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    body: formData
                }).then(async function (response) {
                    const payload = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payload.error || 'Unable to submit investigation');
                    }
                    window.location.reload();
                }).catch(function (error) {
                    Swal.fire({ icon: 'error', title: 'Investigation submission failed', text: error.message });
                });
            });
        }

        document.querySelectorAll('[data-approve-investigation]').forEach(function (button) {
            button.addEventListener('click', function () {
                const reportId = button.getAttribute('data-approve-investigation');
                Swal.fire({
                    title: 'Approve investigation',
                    input: 'textarea',
                    inputLabel: 'Approval comment',
                    inputPlaceholder: 'Optional comment for the next approver',
                    showCancelButton: true,
                    confirmButtonText: 'Approve'
                }).then(function (result) {
                    if (!result.isConfirmed) {
                        return;
                    }
                    submitDecision(reportId, true, result.value || '');
                });
            });
        });

        document.querySelectorAll('[data-deny-investigation]').forEach(function (button) {
            button.addEventListener('click', function () {
                const reportId = button.getAttribute('data-deny-investigation');
                Swal.fire({
                    title: 'Deny investigation',
                    input: 'textarea',
                    inputLabel: 'Reason for denial',
                    inputPlaceholder: 'Comment is required',
                    inputValidator: function (value) {
                        if (!value) {
                            return 'A denial comment is required';
                        }
                    },
                    showCancelButton: true,
                    confirmButtonText: 'Deny'
                }).then(function (result) {
                    if (!result.isConfirmed) {
                        return;
                    }
                    submitDecision(reportId, false, result.value);
                });
            });
        });

        document.querySelectorAll('[data-view-investigation]').forEach(function (button) {
            button.addEventListener('click', function () {
                const reportId = button.getAttribute('data-view-investigation');
                window.fetch(appContext.investigationApiUrl + '/' + reportId + '/logs', {
                    method: 'GET',
                    credentials: 'same-origin'
                }).then(async function (response) {
                    const payload = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payload.error || 'Unable to load investigation history');
                    }
                    const rows = (payload || []).map(function (item) {
                        return '<div class="ui-list-row"><div><div class="fw-semibold">' + item.level + ' - ' + item.status + '</div><div class="small ui-muted">' + (item.comment || 'No comment') + '</div></div><strong>' + item.timestamp + '</strong></div>';
                    }).join('');
                    Swal.fire({
                        title: 'Investigation History',
                        html: rows || '<div class="small ui-muted">No history entries found.</div>',
                        width: 900
                    });
                }).catch(function (error) {
                    Swal.fire({ icon: 'error', title: 'History load failed', text: error.message });
                });
            });
        });

        document.querySelectorAll('[data-pdf-investigation]').forEach(function (button) {
            button.addEventListener('click', function () {
                const reportId = button.getAttribute('data-pdf-investigation');
                const lang = button.getAttribute('data-pdf-lang') || currentLang || 'en';
                window.open(appContext.investigationApiUrl + '/' + reportId + '/pdf?lang=' + encodeURIComponent(lang), '_blank');
            });
        });

        function submitDecision(reportId, approve, comment) {
            window.fetch(appContext.investigationApiUrl + '/' + reportId + '/decision', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ approve: approve, comment: comment })
            }).then(async function (response) {
                const payload = await readResponsePayload(response);
                if (!response.ok) {
                    throw new Error(payload.error || 'Unable to update investigation');
                }
                window.location.reload();
            }).catch(function (error) {
                Swal.fire({ icon: 'error', title: 'Workflow action failed', text: error.message });
            });
        }
    }

    const incidentRecords = Array.isArray(appContext.incidentRecords) ? appContext.incidentRecords : [];
    const equipmentRecords = Array.isArray(appContext.equipmentRecords) ? appContext.equipmentRecords : [];
    const hydrantRecords = Array.isArray(appContext.hydrantRecords) ? appContext.hydrantRecords : [];
    const currentUserId = appContext.currentUserId;

    function escapeHtml(value) {
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function normalizeEmptyValue(value) {
        if (value === null || typeof value === 'undefined' || value === '') {
            return 'Not provided';
        }
        if (typeof value === 'boolean') {
            return value ? 'Yes' : 'No';
        }
        return String(value);
    }

    function detailCard(label, value) {
        return '<div class="ui-detail-card"><div class="ui-detail-label">' + escapeHtml(label) + '</div><div class="ui-detail-value">' + escapeHtml(normalizeEmptyValue(value)) + '</div></div>';
    }

    function fillModalContent(contentId, html) {
        const container = document.getElementById(contentId);
        if (container) {
            container.innerHTML = html;
        }
    }

    function openModal(modalId) {
        const modalElement = document.getElementById(modalId);
        if (!modalElement) {
            return;
        }
        const modal = bootstrap.Modal.getOrCreateInstance(modalElement);
        modal.show();
    }

    function closeModal(modalId) {
        const modalElement = document.getElementById(modalId);
        if (!modalElement) {
            return;
        }
        const modal = bootstrap.Modal.getInstance(modalElement);
        if (modal) {
            modal.hide();
        }
    }

    function numberOrNull(value) {
        if (value === null || typeof value === 'undefined') {
            return null;
        }
        const normalized = String(value).trim();
        if (!normalized) {
            return null;
        }
        const parsed = Number(normalized);
        return Number.isFinite(parsed) ? parsed : null;
    }

    function normalizeDateTimeLocal(value) {
        if (!value) {
            return null;
        }
        return value.length === 16 ? value + ':00' : value;
    }

    function formatDateTimeLocal(value) {
        if (!value) {
            return '';
        }
        return String(value).replace(' ', 'T').slice(0, 16);
    }

    function confirmActionPrompt(title, text, confirmButtonText) {
        return Swal.fire({
            title: title,
            text: text,
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: confirmButtonText || 'Continue',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#f57c00'
        }).then(function (result) {
            return result.isConfirmed;
        });
    }

    function recordById(records, id) {
        return records.find(function (item) {
            return String(item.id) === String(id);
        }) || null;
    }

    document.querySelectorAll('[data-view-incident]').forEach(function (button) {
        button.addEventListener('click', function () {
            const record = recordById(incidentRecords, button.getAttribute('data-view-incident'));
            if (!record) {
                return;
            }
            fillModalContent('incidentDetailContent', [
                '<div class="ui-detail-grid">',
                detailCard('Incident Number', record.incidentNumber),
                detailCard('Report Level', record.reportLevel),
                detailCard('Initial Report Reference', record.parentIncidentNumber),
                detailCard('Linked System Report', record.linkedCallReportNumber),
                detailCard('Operation Category', record.operationCategory),
                detailCard('Incident Type', record.incidentType),
                detailCard('Status', record.status),
                detailCard('Severity', record.severity),
                detailCard('Location', record.location),
                detailCard('Means of Reporting', record.reportingMeans),
                detailCard('Reporter Name', record.reportingPerson),
                detailCard('Reporter Phone', record.reportingContact),
                detailCard('Reported At', record.reportedAt),
                detailCard('Incident Duration', record.incidentDurationMinutes),
                detailCard('Response Time', record.responseTimeMinutes),
                detailCard('Operation Duration', record.operationDurationMinutes),
                detailCard('Other Security Organs', record.otherSecurityOrgans),
                detailCard('Oil Used', record.oilUsedLitres),
                detailCard('Equipment Dispatched', record.equipmentDispatched),
                detailCard('Personnel Dispatched', record.personnelDispatched),
                detailCard('Personnel Names', record.personnelNames),
                detailCard('Supervisor', record.supervisorName),
                detailCard('Commander', record.operationCommander),
                detailCard('Effects On People', record.effectsOnPeople),
                detailCard('Effects On Environment', record.effectsOnEnvironment),
                detailCard('Injured Count', record.casualtiesInjured),
                detailCard('Dead Count', record.casualtiesDead),
                detailCard('Injured Details', record.injuredPeopleDetails),
                detailCard('Dead Details', record.diedPeopleDetails),
                detailCard('Sex / Age Summary', record.casualtyDemographics),
                detailCard('Resources Used', record.resourcesUsed),
                detailCard('Description', record.description),
                detailCard('Action Taken', record.actionTaken),
                detailCard('Outcome', record.outcome),
                detailCard('AI Recommendation', record.aiRecommendation),
                detailCard('Approval Status', record.approvalStatus),
                detailCard('Approval Level', record.approvalCurrentLevel),
                detailCard('Approval Comment', record.approvalLastComment),
                '</div>'
            ].join(''));
            openModal('incidentDetailModal');
        });
    });

    document.querySelectorAll('[data-view-equipment]').forEach(function (button) {
        button.addEventListener('click', function () {
            const record = recordById(equipmentRecords, button.getAttribute('data-view-equipment'));
            if (!record) {
                return;
            }
            fillModalContent('equipmentDetailContent', [
                '<div class="ui-detail-grid">',
                detailCard('Equipment', record.name),
                detailCard('Type', record.typeLabel || record.type),
                detailCard('Serial Number', record.serialNumber),
                detailCard('Condition', record.conditionStatus),
                detailCard('Status', record.operationalStatus),
                detailCard('Date Bought', record.purchaseDate),
                detailCard('Maintenance Due', record.maintenanceDueDate),
                detailCard('Last Maintenance', record.lastServicedAt),
                detailCard('Registered At', record.createdAt),
                detailCard('Maintenance Required', record.maintenanceRequired),
                detailCard('Number In Store', record.quantityInStore),
                detailCard('Station', record.station),
                detailCard('Approval Status', record.approvalStatus),
                detailCard('Approval Level', record.approvalCurrentLevel),
                detailCard('Approval Comment', record.approvalLastComment),
                '</div>'
            ].join(''));
            openModal('equipmentDetailModal');
        });
    });

    document.querySelectorAll('[data-view-hydrant]').forEach(function (button) {
        button.addEventListener('click', function () {
            const record = recordById(hydrantRecords, button.getAttribute('data-view-hydrant'));
            if (!record) {
                return;
            }
            const locations = Array.isArray(record.locations) && record.locations.length > 0
                ? record.locations.map(function (location) {
                    return '<li><strong>' + escapeHtml(normalizeEmptyValue(location.name)) + '</strong> - ' + escapeHtml(normalizeEmptyValue(location.status)) + ' / ' + escapeHtml(normalizeEmptyValue(location.pressure)) + '</li>';
                }).join('')
                : '<li>No hydrant locations listed.</li>';
            fillModalContent('hydrantDetailContent', [
                '<div class="ui-detail-grid">',
                detailCard('Station', record.station),
                detailCard('Region', record.region),
                detailCard('District', record.district),
                detailCard('Total Hydrants', record.totalHydrants),
                detailCard('Working', record.working),
                detailCard('Not Working', record.notWorking),
                detailCard('Low Pressure', record.lowPressure),
                detailCard('Approval Status', record.approvalStatus),
                detailCard('Approval Level', record.approvalCurrentLevel),
                detailCard('Approval Comment', record.approvalLastComment),
                detailCard('Remarks', record.remarks),
                '</div>',
                '<div class="ui-card mt-3"><div class="ui-section-head mb-2"><h3 class="h6 mb-0">Hydrant Locations</h3></div><ul class="ui-manual-list mb-0">' + locations + '</ul></div>'
            ].join(''));
            openModal('hydrantDetailModal');
        });
    });

    const incidentForm = document.getElementById('incidentForm');
    if (incidentForm && appContext.incidentsApiUrl && appContext.canRegisterOperationalIncident) {
        const incidentEditId = document.getElementById('incidentEditId');
        const incidentReportLevel = document.getElementById('incidentReportLevel');
        const incidentLinkedCallId = document.getElementById('incidentLinkedCallId');
        const incidentParentIncidentId = document.getElementById('incidentParentIncidentId');
        const incidentReportingMeans = document.getElementById('incidentReportingMeans');
        const incidentReporterName = document.getElementById('incidentReporterName');
        const incidentReporterPhone = document.getElementById('incidentReporterPhone');
        const incidentWorkflowHintTitle = document.getElementById('incidentWorkflowHintTitle');
        const incidentWorkflowHintText = document.getElementById('incidentWorkflowHintText');

        function toggleIncidentWorkflowMode() {
            const reportLevel = incidentReportLevel ? incidentReportLevel.value : 'INITIAL';
            document.querySelectorAll('[data-incident-initial-only]').forEach(function (element) {
                element.style.display = reportLevel === 'INITIAL' ? '' : 'none';
            });
            document.querySelectorAll('[data-incident-full-only]').forEach(function (element) {
                element.style.display = reportLevel === 'FULL' ? '' : 'none';
            });
            if (incidentWorkflowHintTitle && incidentWorkflowHintText) {
                if (reportLevel === 'FULL') {
                    incidentWorkflowHintTitle.textContent = 'Full incident workflow';
                    incidentWorkflowHintText.textContent = 'Choose an earlier initial report first. A full report cannot exist until its initial report has already been registered.';
                    return;
                }
                incidentWorkflowHintTitle.textContent = 'Initial incident workflow';
                incidentWorkflowHintText.textContent = 'Choose a routed system incident when the station is converting a public or 114 report into the first registered initial report. Physical reports can be entered directly.';
            }
        }

        function applyIncidentCallSelection() {
            if (!incidentLinkedCallId) {
                return;
            }
            const selectedOption = incidentLinkedCallId.options[incidentLinkedCallId.selectedIndex];
            if (!selectedOption || !selectedOption.value) {
                return;
            }
            const callerName = selectedOption.getAttribute('data-caller-name') || '';
            const callerNumber = selectedOption.getAttribute('data-caller-number') || '';
            const reportingMeans = selectedOption.getAttribute('data-reporting-means') || '';
            const location = selectedOption.getAttribute('data-location') || '';
            if (incidentReporterName && !incidentReporterName.value.trim()) {
                incidentReporterName.value = callerName;
            }
            if (incidentReporterPhone && !incidentReporterPhone.value.trim()) {
                incidentReporterPhone.value = callerNumber;
            }
            if (incidentReportingMeans && reportingMeans) {
                incidentReportingMeans.value = reportingMeans.toUpperCase().replace('-', '_').replace(' ', '_');
            }
            const incidentLocationDetails = document.getElementById('incidentLocationDetails');
            if (incidentLocationDetails && !incidentLocationDetails.value.trim()) {
                incidentLocationDetails.value = location;
            }
        }

        if (incidentReportLevel) {
            incidentReportLevel.addEventListener('change', toggleIncidentWorkflowMode);
            toggleIncidentWorkflowMode();
        }

        if (incidentLinkedCallId) {
            incidentLinkedCallId.addEventListener('change', applyIncidentCallSelection);
        }

        function resetIncidentEditorState() {
            if (incidentEditId) {
                incidentEditId.value = '';
            }
            const titleElement = incidentForm.closest('.modal-content')?.querySelector('.modal-title');
            if (titleElement) {
                titleElement.textContent = 'Register Incident';
            }
        }

        function loadIncidentForEditing(record) {
            if (!record) {
                return;
            }
            if (incidentEditId) {
                incidentEditId.value = record.id;
            }
            const titleElement = incidentForm.closest('.modal-content')?.querySelector('.modal-title');
            if (titleElement) {
                titleElement.textContent = 'Return and Edit Incident';
            }
            if (incidentReportLevel) {
                incidentReportLevel.value = record.reportLevel || 'INITIAL';
                toggleIncidentWorkflowMode();
            }
            if (incidentLinkedCallId) {
                incidentLinkedCallId.value = record.linkedCallId || '';
            }
            if (incidentParentIncidentId) {
                incidentParentIncidentId.value = record.parentIncidentId || '';
            }
            document.getElementById('incidentOperationCategory').value = record.operationCategory || 'GENERAL';
            document.getElementById('incidentTypeInput').value = record.incidentType || '';
            document.getElementById('incidentSeverity').value = record.severity || 'LOW';
            if (incidentReportingMeans) {
                incidentReportingMeans.value = record.reportingMeans || 'PHYSICAL';
            }
            if (incidentReporterName) {
                incidentReporterName.value = record.reportingPerson || '';
            }
            if (incidentReporterPhone) {
                incidentReporterPhone.value = record.reportingContact || '';
            }
            document.getElementById('incidentStatus').value = record.status || 'RECEIVED';
            document.getElementById('incidentLocationDetails').value = record.location || '';
            document.getElementById('incidentVillage').value = record.village || '';
            document.getElementById('incidentReportedAt').value = formatDateTimeLocal(record.reportedAt);
            document.getElementById('incidentResponseTime').value = record.responseTimeMinutes || '';
            document.getElementById('incidentDuration').value = record.incidentDurationMinutes || '';
            document.getElementById('incidentOperationDuration').value = record.operationDurationMinutes || '';
            document.getElementById('incidentOilUsed').value = record.oilUsedLitres || '';
            document.getElementById('incidentOtherOrgans').value = record.otherSecurityOrgans || '';
            document.getElementById('incidentInjuredCount').value = record.casualtiesInjured || '';
            document.getElementById('incidentDeadCount').value = record.casualtiesDead || '';
            document.getElementById('incidentEquipmentDispatched').value = record.equipmentDispatched || '';
            document.getElementById('incidentPersonnelDispatched').value = record.personnelDispatched || '';
            document.getElementById('incidentPersonnelNames').value = record.personnelNames || '';
            document.getElementById('incidentSupervisorName').value = record.supervisorName || '';
            document.getElementById('incidentCommander').value = record.operationCommander || '';
            document.getElementById('incidentDescription').value = record.description || '';
            document.getElementById('incidentResourcesUsed').value = record.resourcesUsed || '';
            document.getElementById('incidentEffectsPeople').value = record.effectsOnPeople || '';
            document.getElementById('incidentEffectsEnvironment').value = record.effectsOnEnvironment || '';
            document.getElementById('incidentInjuredDetails').value = record.injuredPeopleDetails || '';
            document.getElementById('incidentDeadDetails').value = record.diedPeopleDetails || '';
            document.getElementById('incidentDemographics').value = record.casualtyDemographics || '';
            document.getElementById('incidentActionTaken').value = record.actionTaken || '';
            document.getElementById('incidentOutcome').value = record.outcome || '';
            openModal('incidentModal');
        }

        document.querySelectorAll('[data-edit-incident]').forEach(function (button) {
            button.addEventListener('click', function () {
                loadIncidentForEditing(recordById(incidentRecords, button.getAttribute('data-edit-incident')));
            });
        });

        incidentForm.addEventListener('submit', function (event) {
            event.preventDefault();
            const editId = incidentEditId ? incidentEditId.value : '';
            const payload = {
                reportLevel: incidentReportLevel ? incidentReportLevel.value : 'INITIAL',
                linkedCallId: incidentLinkedCallId && incidentLinkedCallId.value ? Number(incidentLinkedCallId.value) : null,
                parentIncidentId: incidentParentIncidentId && incidentParentIncidentId.value ? Number(incidentParentIncidentId.value) : null,
                operationCategory: document.getElementById('incidentOperationCategory').value,
                incidentType: document.getElementById('incidentTypeInput').value.trim(),
                severity: document.getElementById('incidentSeverity').value,
                reportingMeans: incidentReportingMeans ? incidentReportingMeans.value : 'PHYSICAL',
                reportingPerson: incidentReporterName ? incidentReporterName.value.trim() : '',
                reportingContact: incidentReporterPhone ? incidentReporterPhone.value.trim() : '',
                status: document.getElementById('incidentStatus').value,
                locationDetails: document.getElementById('incidentLocationDetails').value.trim(),
                village: document.getElementById('incidentVillage').value.trim(),
                reportedAt: normalizeDateTimeLocal(document.getElementById('incidentReportedAt').value),
                responseTimeMinutes: numberOrNull(document.getElementById('incidentResponseTime').value),
                incidentDurationMinutes: numberOrNull(document.getElementById('incidentDuration').value),
                operationDurationMinutes: numberOrNull(document.getElementById('incidentOperationDuration').value),
                oilUsedLitres: numberOrNull(document.getElementById('incidentOilUsed').value),
                otherSecurityOrgans: document.getElementById('incidentOtherOrgans').value.trim(),
                casualtiesInjured: numberOrNull(document.getElementById('incidentInjuredCount').value),
                casualtiesDead: numberOrNull(document.getElementById('incidentDeadCount').value),
                equipmentDispatched: document.getElementById('incidentEquipmentDispatched').value.trim(),
                personnelDispatched: document.getElementById('incidentPersonnelDispatched').value.trim(),
                personnelNames: document.getElementById('incidentPersonnelNames').value.trim(),
                supervisorName: document.getElementById('incidentSupervisorName').value.trim(),
                operationCommander: document.getElementById('incidentCommander').value.trim(),
                description: document.getElementById('incidentDescription').value.trim(),
                resourcesUsed: document.getElementById('incidentResourcesUsed').value.trim(),
                effectsOnPeople: document.getElementById('incidentEffectsPeople').value.trim(),
                effectsOnEnvironment: document.getElementById('incidentEffectsEnvironment').value.trim(),
                injuredPeopleDetails: document.getElementById('incidentInjuredDetails').value.trim(),
                diedPeopleDetails: document.getElementById('incidentDeadDetails').value.trim(),
                casualtyDemographics: document.getElementById('incidentDemographics').value.trim(),
                actionTaken: document.getElementById('incidentActionTaken').value.trim(),
                outcome: document.getElementById('incidentOutcome').value.trim()
            };
            confirmActionPrompt(
                editId
                    ? 'Return this incident for approval again?'
                    : payload.reportLevel === 'FULL' ? 'Save full incident report?' : 'Save initial incident report?',
                editId
                    ? 'Your changes will be saved on the same incident and sent back through the approval workflow.'
                    : payload.reportLevel === 'FULL'
                        ? 'This full report will be linked to the selected initial report and sent through the approval workflow.'
                        : 'This incident will be registered and sent through the approval workflow.',
                editId ? 'Resubmit incident' : payload.reportLevel === 'FULL' ? 'Save full report' : 'Save incident'
            ).then(function (confirmed) {
                if (!confirmed) {
                    return null;
                }
                return window.fetch(editId ? (appContext.incidentsApiUrl + '/' + editId + '/resubmit') : appContext.incidentsApiUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                }).then(async function (response) {
                    const payloadData = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payloadData.error || 'Unable to save incident');
                    }
                    closeModal('incidentModal');
                    resetIncidentEditorState();
                    return Swal.fire({ icon: 'success', title: editId ? 'Incident resubmitted' : 'Incident saved', text: editId ? 'The incident was updated and returned to the approval workflow.' : 'The incident has been registered successfully.' });
                }).then(function () {
                    window.location.reload();
                });
            }).catch(function (error) {
                if (error) {
                    Swal.fire({ icon: 'error', title: 'Incident save failed', text: error.message });
                }
            });
        });
    }

    const equipmentForm = document.getElementById('equipmentForm');
    if (equipmentForm && appContext.equipmentApiUrl && appContext.canManageEquipment) {
        const equipmentEditId = document.getElementById('equipmentEditId');
        const equipmentModalTitle = equipmentForm.closest('.modal-content')?.querySelector('.modal-title');

        function resetEquipmentEditorState() {
            if (equipmentEditId) {
                equipmentEditId.value = '';
            }
            if (equipmentModalTitle) {
                equipmentModalTitle.textContent = 'Register Equipment';
            }
        }

        document.querySelectorAll('[data-edit-equipment]').forEach(function (button) {
            button.addEventListener('click', function () {
                const record = recordById(equipmentRecords, button.getAttribute('data-edit-equipment'));
                if (!record) {
                    return;
                }
                if (equipmentEditId) {
                    equipmentEditId.value = record.id;
                }
                if (equipmentModalTitle) {
                    equipmentModalTitle.textContent = 'Return and Edit Equipment';
                }
                document.getElementById('equipmentNameInput').value = record.name || '';
                document.getElementById('equipmentTypeInput').value = record.type || 'FIRE_FIGHTING_EQUIPMENT';
                document.getElementById('equipmentSerialNumber').value = record.serialNumber === 'Not provided' ? '' : (record.serialNumber || '');
                document.getElementById('equipmentConditionStatus').value = record.conditionStatus || 'GOOD';
                document.getElementById('equipmentOperationalStatus').value = record.operationalStatus || 'AVAILABLE';
                document.getElementById('equipmentPurchaseDate').value = record.purchaseDate || '';
                document.getElementById('equipmentLastServicedAt').value = formatDateTimeLocal(record.lastServicedAt);
                document.getElementById('equipmentQuantityInStore').value = record.quantityInStore || 1;
                document.getElementById('equipmentMaintenanceRequired').checked = Boolean(record.maintenanceRequired);
                openModal('equipmentModal');
            });
        });

        equipmentForm.addEventListener('submit', function (event) {
            event.preventDefault();
            const editId = equipmentEditId ? equipmentEditId.value : '';
            const payload = {
                name: document.getElementById('equipmentNameInput').value.trim(),
                type: document.getElementById('equipmentTypeInput').value,
                serialNumber: document.getElementById('equipmentSerialNumber').value.trim(),
                conditionStatus: document.getElementById('equipmentConditionStatus').value,
                operationalStatus: document.getElementById('equipmentOperationalStatus').value,
                purchaseDate: document.getElementById('equipmentPurchaseDate').value || null,
                lastServicedAt: normalizeDateTimeLocal(document.getElementById('equipmentLastServicedAt').value),
                quantityInStore: numberOrNull(document.getElementById('equipmentQuantityInStore').value) || 1,
                maintenanceRequired: document.getElementById('equipmentMaintenanceRequired').checked
            };
            confirmActionPrompt(
                editId ? 'Return this equipment record for approval again?' : 'Save equipment record?',
                editId ? 'Your changes will be saved on the same equipment record and returned to the approval workflow.' : 'This equipment record will be registered and sent through the approval workflow.',
                editId ? 'Resubmit equipment' : 'Save equipment'
            ).then(function (confirmed) {
                if (!confirmed) {
                    return null;
                }
                return window.fetch(editId ? (appContext.equipmentApiUrl + '/' + editId + '/resubmit') : appContext.equipmentApiUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                }).then(async function (response) {
                    const payloadData = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payloadData.error || 'Unable to save equipment');
                    }
                    closeModal('equipmentModal');
                    resetEquipmentEditorState();
                    return Swal.fire({ icon: 'success', title: editId ? 'Equipment resubmitted' : 'Equipment saved', text: editId ? 'The equipment record was updated and returned to the approval workflow.' : 'The equipment record has been registered successfully.' });
                }).then(function () {
                    window.location.reload();
                });
            }).catch(function (error) {
                if (error) {
                    Swal.fire({ icon: 'error', title: 'Equipment save failed', text: error.message });
                }
            });
        });
    }

    const hydrantForm = document.getElementById('hydrantForm');
    if (hydrantForm && appContext.hydrantApiUrl && appContext.canManageHydrants) {
        hydrantForm.addEventListener('submit', function (event) {
            event.preventDefault();
            const locationLines = document.getElementById('hydrantLocations').value.split('\n');
            const locations = locationLines.map(function (line) {
                const parts = line.split('|').map(function (item) { return item.trim(); });
                if (!parts[0]) {
                    return null;
                }
                return {
                    name: parts[0],
                    status: parts[1] || '',
                    pressure: parts[2] || ''
                };
            }).filter(Boolean);
            const payload = {
                working: numberOrNull(document.getElementById('hydrantWorking').value) || 0,
                notWorking: numberOrNull(document.getElementById('hydrantNotWorking').value) || 0,
                lowPressure: numberOrNull(document.getElementById('hydrantLowPressure').value) || 0,
                remarks: document.getElementById('hydrantRemarks').value.trim(),
                locations: locations
            };
            confirmActionPrompt('Save hydrant report?', 'This hydrant report will be registered and sent through the approval workflow.', 'Save hydrant report').then(function (confirmed) {
                if (!confirmed) {
                    return null;
                }
                return window.fetch(appContext.hydrantApiUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(payload)
                }).then(async function (response) {
                    const payloadData = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payloadData.error || 'Unable to save hydrant report');
                    }
                    closeModal('hydrantModal');
                    return Swal.fire({ icon: 'success', title: 'Hydrant report saved', text: 'The hydrant report has been registered successfully.' });
                }).then(function () {
                    window.location.reload();
                });
            }).catch(function (error) {
                if (error) {
                    Swal.fire({ icon: 'error', title: 'Hydrant save failed', text: error.message });
                }
            });
        });
    }

    function submitOperationalDecision(url, approve, successTitle) {
        const config = approve
            ? {
                title: 'Approve submission',
                inputLabel: 'Approval comment',
                inputPlaceholder: 'Optional forwarding comment',
                confirmButtonText: 'Approve'
            }
            : {
                title: 'Deny submission',
                inputLabel: 'Reason for denial',
                inputPlaceholder: 'Comment is required',
                confirmButtonText: 'Deny'
            };
        Swal.fire({
            title: config.title,
            input: 'textarea',
            inputLabel: config.inputLabel,
            inputPlaceholder: config.inputPlaceholder,
            inputValidator: approve ? null : function (value) {
                if (!value) {
                    return 'A denial comment is required';
                }
            },
            showCancelButton: true,
            confirmButtonText: config.confirmButtonText
        }).then(function (result) {
            if (!result.isConfirmed) {
                return null;
            }
            return window.fetch(url, {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ approve: approve, comment: result.value || '' })
            }).then(async function (response) {
                const payloadData = await readResponsePayload(response);
                if (!response.ok) {
                    throw new Error(payloadData.error || 'Unable to complete workflow action');
                }
                return Swal.fire({ icon: 'success', title: successTitle, text: 'The workflow action was completed successfully.' });
            }).then(function () {
                window.location.reload();
            });
        }).catch(function (error) {
            Swal.fire({ icon: 'error', title: 'Workflow action failed', text: error.message });
        });
    }

    document.querySelectorAll('[data-approve-incident]').forEach(function (button) {
        button.addEventListener('click', function () {
            submitOperationalDecision(appContext.incidentsApiUrl + '/' + button.getAttribute('data-approve-incident') + '/decision', true, 'Incident approved');
        });
    });

    document.querySelectorAll('[data-deny-incident]').forEach(function (button) {
        button.addEventListener('click', function () {
            submitOperationalDecision(appContext.incidentsApiUrl + '/' + button.getAttribute('data-deny-incident') + '/decision', false, 'Incident denied');
        });
    });

    document.querySelectorAll('[data-approve-equipment]').forEach(function (button) {
        button.addEventListener('click', function () {
            submitOperationalDecision(appContext.equipmentApiUrl + '/' + button.getAttribute('data-approve-equipment') + '/decision', true, 'Equipment approved');
        });
    });

    document.querySelectorAll('[data-deny-equipment]').forEach(function (button) {
        button.addEventListener('click', function () {
            submitOperationalDecision(appContext.equipmentApiUrl + '/' + button.getAttribute('data-deny-equipment') + '/decision', false, 'Equipment denied');
        });
    });

    document.querySelectorAll('[data-approve-hydrant]').forEach(function (button) {
        button.addEventListener('click', function () {
            submitOperationalDecision(appContext.hydrantApiUrl + '/' + button.getAttribute('data-approve-hydrant') + '/decision', true, 'Hydrant report approved');
        });
    });

    document.querySelectorAll('[data-deny-hydrant]').forEach(function (button) {
        button.addEventListener('click', function () {
            submitOperationalDecision(appContext.hydrantApiUrl + '/' + button.getAttribute('data-deny-hydrant') + '/decision', false, 'Hydrant report denied');
        });
    });

    const teleSupportIncidentId = document.getElementById('teleSupportIncidentId');
    const teleSupportRequestButton = document.getElementById('teleSupportRequestButton');
    if (teleSupportRequestButton && appContext.teleSupportRequestUrlTemplate) {
        teleSupportRequestButton.addEventListener('click', function () {
            if (!teleSupportIncidentId || !teleSupportIncidentId.value) {
                Swal.fire({ icon: 'error', title: 'Incident required', text: 'Select the active incident that needs tele-support first.' });
                return;
            }
            Swal.fire({
                title: 'Request tele-support',
                input: 'textarea',
                inputLabel: 'Request note',
                inputPlaceholder: 'Describe what specialist guidance is needed at the scene',
                showCancelButton: true,
                confirmButtonText: 'Send request'
            }).then(function (result) {
                if (!result.isConfirmed) {
                    return null;
                }
                const requestUrl = appContext.teleSupportRequestUrlTemplate.replace('__id__', String(teleSupportIncidentId.value));
                return window.fetch(requestUrl, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ message: result.value || '' })
                }).then(async function (response) {
                    const payload = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payload.error || 'Unable to send tele-support request');
                    }
                    return Swal.fire({
                        icon: 'success',
                        title: 'Tele-support requested',
                        text: 'Specialist dashboards have been notified and the request alarm has been triggered.'
                    });
                });
            }).catch(function (error) {
                if (error) {
                    Swal.fire({ icon: 'error', title: 'Tele-support request failed', text: error.message });
                }
            });
        });
    }

    if (appContext.canManageGeography && appContext.geographyApiUrl) {
        const districtRegionId = document.getElementById('districtRegionId');
        const stationRegionId = document.getElementById('stationRegionId');
        const stationDistrictId = document.getElementById('stationDistrictId');
        const stationName = document.getElementById('stationName');
        const districtModal = document.getElementById('districtModal');
        const stationModal = document.getElementById('stationModal');

        function fillRegionSelect(selectElement) {
            if (!selectElement) {
                return;
            }
            resetSelectOptions(selectElement, 'Select region');
            (appContext.regions || []).forEach(function (region) {
                const option = document.createElement('option');
                option.value = String(region.id);
                option.textContent = region.name;
                selectElement.appendChild(option);
            });
        }

        function resetDistrictSelect(selectElement) {
            if (!selectElement) {
                return;
            }
            resetSelectOptions(selectElement, 'Select district');
            selectElement.disabled = true;
        }

        async function loadDistrictOptions(regionId, selectElement) {
            if (!selectElement) {
                return;
            }
            resetDistrictSelect(selectElement);
            if (!regionId) {
                return;
            }
            const response = await window.fetch(appContext.geographyApiUrl + '/regions/' + regionId + '/districts', {
                method: 'GET',
                credentials: 'same-origin'
            });
            const payload = await readResponsePayload(response);
            if (!response.ok) {
                throw new Error(payload.error || 'Unable to load districts');
            }
            payload.forEach(function (district) {
                const option = document.createElement('option');
                option.value = String(district.id);
                option.textContent = district.name;
                selectElement.appendChild(option);
            });
            selectElement.disabled = false;
        }

        function syncStationName() {
            if (!stationDistrictId || !stationName) {
                return;
            }
            const selectedOption = stationDistrictId.options[stationDistrictId.selectedIndex];
            if (!stationDistrictId.value || !selectedOption) {
                stationName.value = '';
                return;
            }
            stationName.value = selectedOption.textContent + ' Fire Station';
        }

        fillRegionSelect(districtRegionId);
        fillRegionSelect(stationRegionId);
        resetDistrictSelect(stationDistrictId);

        if (districtModal && districtRegionId) {
            districtModal.addEventListener('show.bs.modal', function () {
                const districtForm = document.getElementById('districtForm');
                if (districtForm) {
                    districtForm.reset();
                }
                fillRegionSelect(districtRegionId);
            });
        }

        if (stationRegionId && stationDistrictId) {
            stationRegionId.addEventListener('change', function () {
                loadDistrictOptions(stationRegionId.value, stationDistrictId).catch(function (error) {
                    Swal.fire({ icon: 'error', title: 'District load failed', text: error.message });
                }).finally(function () {
                    syncStationName();
                });
            });
        }

        if (stationDistrictId) {
            stationDistrictId.addEventListener('change', syncStationName);
        }

        if (stationModal && stationRegionId && stationDistrictId) {
            stationModal.addEventListener('show.bs.modal', function () {
                const stationForm = document.getElementById('stationForm');
                if (stationForm) {
                    stationForm.reset();
                }
                fillRegionSelect(stationRegionId);
                resetDistrictSelect(stationDistrictId);
                syncStationName();
            });
        }

        const regionForm = document.getElementById('regionForm');
        if (regionForm) {
            regionForm.addEventListener('submit', function (event) {
                event.preventDefault();
                confirmActionPrompt('Register region?', 'This region will become available in geography routing and administration workflows.', 'Save region').then(function (confirmed) {
                    if (!confirmed) {
                        return null;
                    }
                    return window.fetch(appContext.geographyApiUrl + '/regions', {
                        method: 'POST',
                        credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            name: document.getElementById('regionName').value,
                            code: document.getElementById('regionCode').value
                        })
                    }).then(async function (response) {
                        const payload = await readResponsePayload(response);
                        if (!response.ok) {
                            throw new Error(payload.error || 'Unable to save region');
                        }
                        window.location.reload();
                    });
                }).catch(function (error) {
                    if (error) {
                        Swal.fire({ icon: 'error', title: 'Region save failed', text: error.message });
                    }
                });
            });
        }

        const districtForm = document.getElementById('districtForm');
        if (districtForm) {
            districtForm.addEventListener('submit', function (event) {
                event.preventDefault();
                confirmActionPrompt('Register district?', 'This district will be added to the selected region and become available for routing.', 'Save district').then(function (confirmed) {
                    if (!confirmed) {
                        return null;
                    }
                    return window.fetch(appContext.geographyApiUrl + '/districts', {
                        method: 'POST',
                        credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            regionId: Number(document.getElementById('districtRegionId').value),
                            name: document.getElementById('districtName').value
                        })
                    }).then(async function (response) {
                        const payload = await readResponsePayload(response);
                        if (!response.ok) {
                            throw new Error(payload.error || 'Unable to save district');
                        }
                        window.location.reload();
                    });
                }).catch(function (error) {
                    if (error) {
                        Swal.fire({ icon: 'error', title: 'District save failed', text: error.message });
                    }
                });
            });
        }

        const stationForm = document.getElementById('stationForm');
        if (stationForm) {
            stationForm.addEventListener('submit', function (event) {
                event.preventDefault();
                if (!stationRegionId || !stationRegionId.value) {
                    Swal.fire({ icon: 'error', title: 'Region required', text: 'Select a region before registering a station.' });
                    return;
                }
                if (!stationDistrictId || !stationDistrictId.value) {
                    Swal.fire({ icon: 'error', title: 'District required', text: 'Select a district from the chosen region before registering a station.' });
                    return;
                }
                confirmActionPrompt('Register station?', 'This station will be available for public routing, control-room assignment, and operational visibility.', 'Save station').then(function (confirmed) {
                    if (!confirmed) {
                        return null;
                    }
                    return window.fetch(appContext.geographyApiUrl + '/stations', {
                        method: 'POST',
                        credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({
                            districtId: Number(stationDistrictId.value),
                            name: document.getElementById('stationName').value,
                            village: document.getElementById('stationVillage').value,
                            controlRoomNumber: document.getElementById('stationControlRoomNumber').value,
                            phoneNumber: document.getElementById('stationPhoneNumber').value
                        })
                    }).then(async function (response) {
                        const payload = await readResponsePayload(response);
                        if (!response.ok) {
                            throw new Error(payload.error || 'Unable to save station');
                        }
                        window.location.reload();
                    });
                }).catch(function (error) {
                    if (error) {
                        Swal.fire({ icon: 'error', title: 'Station save failed', text: error.message });
                    }
                });
            });
        }
    }

    if (appContext.canRunSystemTests && appContext.systemTestApiUrl) {
        const runSystemTestButton = document.getElementById('runSystemTestButton');
        const systemTestSummary = document.getElementById('systemTestSummary');
        const systemTestDetailFeed = document.getElementById('systemTestDetailFeed');

        if (runSystemTestButton) {
            runSystemTestButton.addEventListener('click', function () {
                const stages = [
                    'Scanning registered users and access activity',
                    'Checking password strength and failed login history',
                    'Reviewing geography, incidents, runtime routes, and routing integrity',
                    'Checking Python AI availability and recommendation readiness',
                    'Measuring performance and response-rate indicators',
                    'Compiling AI executive findings and generating the report'
                ];
                let stageIndex = 0;
                let stageTimerId = null;

                Swal.fire({
                    title: 'System testing in progress',
                    html: '<div id="systemTestProgressText" class="fw-semibold">Initializing verification engine</div><div class="small ui-muted mt-2">Functionality, visualization, security, and performance checks are running.</div>',
                    allowOutsideClick: false,
                    allowEscapeKey: false,
                    showConfirmButton: false,
                    didOpen: function () {
                        Swal.showLoading();
                        const progressText = Swal.getHtmlContainer().querySelector('#systemTestProgressText');
                        stageTimerId = window.setInterval(function () {
                            stageIndex = (stageIndex + 1) % stages.length;
                            if (progressText) {
                                progressText.textContent = stages[stageIndex];
                            }
                        }, 900);
                    },
                    willClose: function () {
                        if (stageTimerId !== null) {
                            window.clearInterval(stageTimerId);
                        }
                    }
                });

                window.fetch(appContext.systemTestApiUrl + '/run', {
                    method: 'POST',
                    credentials: 'same-origin'
                }).then(async function (response) {
                    const payload = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payload.error || 'Unable to run verification');
                    }
                    Swal.close();
                    Swal.fire({
                        icon: 'success',
                        title: 'System test completed',
                        text: (payload.summary || 'Verification report generated successfully.') + ' The report is now available below.'
                    }).then(function () {
                        window.location.reload();
                    });
                    return null;
                }).catch(function (error) {
                    Swal.close();
                    Swal.fire({ icon: 'error', title: 'Verification failed', text: error.message });
                });
            });
        }

        document.querySelectorAll('[data-view-system-test]').forEach(function (button) {
            button.addEventListener('click', function () {
                const reportId = button.getAttribute('data-view-system-test');
                if (systemTestDetailFeed) {
                    renderLoadingSkeleton(systemTestDetailFeed, 4);
                }
                window.fetch(appContext.systemTestApiUrl + '/' + reportId, {
                    method: 'GET',
                    credentials: 'same-origin'
                }).then(async function (response) {
                    const payload = await readResponsePayload(response);
                    if (!response.ok) {
                        throw new Error(payload.error || 'Unable to load report');
                    }
                    if (systemTestSummary) {
                        systemTestSummary.textContent = payload.report.summary || payload.report.reportNumber;
                    }
                    if (systemTestDetailFeed) {
                        renderListRows(systemTestDetailFeed, payload.details || [], 'No details available.', function (item) {
                            return createListRowElement(item.name || 'Verification check', item.message || '', item.status || 'UNKNOWN');
                        });
                    }
                    const modalElement = document.getElementById('systemTestModal');
                    if (modalElement) {
                        new bootstrap.Modal(modalElement).show();
                    }
                }).catch(function (error) {
                    Swal.fire({ icon: 'error', title: 'Report load failed', text: error.message });
                });
            });
        });

        document.querySelectorAll('[data-download-system-test]').forEach(function (button) {
            button.addEventListener('click', function () {
                const reportId = button.getAttribute('data-download-system-test');
                const lang = button.getAttribute('data-pdf-lang') || currentLang || 'en';
                window.open(appContext.systemTestApiUrl + '/' + reportId + '/pdf?lang=' + encodeURIComponent(lang), '_blank');
            });
        });
    }

    const localVideo = document.getElementById('localVideo');
    const remoteVideo = document.getElementById('remoteVideo');
    const signalStatus = document.getElementById('videoSignalStatus');
    const officerLabel = document.getElementById('videoOfficerLabel');
    const locationLabel = document.getElementById('videoLocationLabel');
    const realtimeFeed = document.getElementById('realtimeFeed');
    const sessionFeed = document.getElementById('videoSessionFeed');
    const alarmModalElement = document.getElementById('dashboardAlarmModal');
    const alarmTitle = document.getElementById('dashboardAlarmTitle');
    const alarmMessage = document.getElementById('dashboardAlarmMessage');
    const alarmMeta = document.getElementById('dashboardAlarmMeta');
    const alarmVideoModalElement = document.getElementById('alarmVideoModal');
    const alarmRemoteVideo = document.getElementById('alarmRemoteVideo');
    const alarmVideoTitle = document.getElementById('alarmVideoTitle');
    const alarmVideoMeta = document.getElementById('alarmVideoMeta');
    const alarmVideoOfficer = document.getElementById('alarmVideoOfficer');
    const alarmVideoLocation = document.getElementById('alarmVideoLocation');
    const startButton = document.querySelector('[data-start-stream]');
    const endButton = document.querySelector('[data-end-stream]');
    const remoteAudioButton = document.querySelector('[data-toggle-remote-audio]');
    const streamAudioMode = document.querySelector('[data-stream-audio-mode]');
    const uploadInput = document.querySelector('[data-video-upload]');

    let socket = null;
    let signalReadyPromise = null;
    let peerConnection = null;
    let localStream = null;
    let responseAudioStream = null;
    let mediaRecorder = null;
    let recordedChunks = [];
    let currentVideoSessionId = null;
    let isPublishingLive = false;
    let remoteAudioMuted = false;
    let pendingTeleSupportIncidentId = null;
    let pendingStationCallId = null;
    let alarmAudioContext = null;
    let alarmAudioPlayer = null;
    const alarmAudioUrl = '/audio/froms-alarm.mp4';
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

    function updateSignalStatus(text) {
        if (signalStatus) {
            signalStatus.textContent = text;
        }
    }

    function syncRemoteAudioState() {
        if (!remoteVideo) {
            return;
        }
        remoteVideo.muted = isPublishingLive || remoteAudioMuted;
        if (alarmRemoteVideo) {
            alarmRemoteVideo.muted = isPublishingLive || remoteAudioMuted;
        }
        if (remoteAudioButton) {
            remoteAudioButton.textContent = remoteVideo.muted ? 'Unmute Speaker' : 'Mute Speaker';
        }
    }

    function appendFeed(container, title, status) {
        if (!container) {
            return;
        }
        const row = createStatusFeedRow(title, status);
        container.prepend(row);
        while (container.children.length > 8) {
            container.removeChild(container.lastChild);
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

    function participantLabel(payload) {
        if (payload && payload.officer) {
            return payload.officer;
        }
        const participantType = payload && payload.participantType ? String(payload.participantType).toUpperCase() : '';
        if (participantType === 'CONTROL_ROOM') {
            return 'Control Room Attendant';
        }
        if (participantType === 'PUBLIC') {
            return 'Public Reporter';
        }
        return 'Live Operator';
    }

    function showAlarm(title, message, meta, options) {
        if (alarmTitle) {
            alarmTitle.textContent = title;
        }
        if (alarmMessage) {
            alarmMessage.textContent = message;
        }
        if (alarmMeta) {
            alarmMeta.textContent = meta || 'Realtime emergency alert';
        }
        if (alarmModalElement && typeof bootstrap !== 'undefined') {
            bootstrap.Modal.getOrCreateInstance(alarmModalElement).show();
        }
        const alarmOptions = options || {};
        playAlarmSound(alarmOptions.profile || 'standard');
        speakAlarm(message, alarmOptions);
    }

    function openAlarmVideoPopup(title, meta, officer, location) {
        if (alarmVideoTitle) {
            alarmVideoTitle.textContent = title;
        }
        if (alarmVideoMeta) {
            alarmVideoMeta.textContent = meta || 'Live emergency popup feed';
        }
        if (alarmVideoOfficer) {
            alarmVideoOfficer.textContent = officer || 'Live Operator';
        }
        if (alarmVideoLocation) {
            alarmVideoLocation.textContent = location || 'Live location';
        }
        if (alarmVideoModalElement && typeof bootstrap !== 'undefined') {
            bootstrap.Modal.getOrCreateInstance(alarmVideoModalElement).show();
        }
    }

    function handlePublicCallAlert(payload) {
        if (!payload || !matchesScope(payload)) {
            return;
        }
        pendingStationCallId = payload.callId || pendingStationCallId;
        showAlarm(
            'Public emergency alert',
            payload.alertMessage || 'FIRE STATION FIRE STATION PUBLIC CALLING EMERGENCY PLEASE',
            joinMeta([
                payload.reportNumber,
                payload.stationName,
                payload.incidentType,
                payload.location
            ]),
            { profile: 'critical', rate: 0.88, pitch: 1.05 }
        );
        appendFeed(realtimeFeed, 'Public report ' + compactText(payload.reportNumber || 'received'), payload.incidentType || 'PUBLIC');
    }

    function handlePublicMessageAlert(payload) {
        if (!payload || !matchesScope(payload) || compactText(payload.senderType).toUpperCase() !== 'PUBLIC') {
            return;
        }
        pendingStationCallId = payload.callId || pendingStationCallId;
        showAlarm(
            'Public message from scene',
            payload.alertMessage || 'FIRE STATION FIRE STATION PUBLIC CALLING EMERGENCY PLEASE',
            joinMeta([
                payload.reportNumber,
                payload.message
            ]),
            { profile: 'critical', rate: 0.88, pitch: 1.04 }
        );
        appendFeed(realtimeFeed, 'Public message for ' + compactText(payload.reportNumber || 'active report'), 'MESSAGE');
    }

    function handleCallAcceptedAlert(payload) {
        if (!payload || !matchesScope(payload)) {
            return;
        }
        pendingStationCallId = payload.callId || pendingStationCallId;
        showAlarm(
            'Station dispatch alert',
            'STATION OPERATION OFFICER STATION OPERATION OFFICER NEW EMERGENCY CALL RECEIVED',
            joinMeta([
                payload.reportNumber,
                payload.station,
                payload.incidentType
            ]),
            { profile: 'dispatch', rate: 0.9, pitch: 1.02 }
        );
        appendFeed(realtimeFeed, 'Dispatch routed to ' + compactText(payload.station || 'station'), payload.incidentType || 'DISPATCH');
    }

    function handleTeleSupportAlert(payload) {
        if (!payload || !isTeleSupportSpecialistRole()) {
            return;
        }
        pendingTeleSupportIncidentId = payload.incidentId || null;
        const requesterRole = compactText(payload.requesterRole).toUpperCase();
        const highUrgencyRequester = requesterRole === 'STATION_OPERATION_OFFICER'
            || requesterRole === 'STATION_FIRE_OPERATION_OFFICER';
        showAlarm(
            'Tele-support request',
            payload.alertMessage || 'HELLO SPECIALIST AGENT NEED TELESUPPORT',
            joinMeta([
                payload.incidentNumber,
                payload.incidentType,
                payload.requester,
                payload.location
            ]),
            {
                profile: highUrgencyRequester ? 'teleSupportCritical' : 'teleSupport',
                rate: highUrgencyRequester ? 0.86 : 0.92,
                pitch: highUrgencyRequester ? 1.08 : 1.0
            }
        );
        openAlarmVideoPopup(
            'Tele-support request received',
            joinMeta([
                payload.incidentNumber,
                payload.status,
                payload.severity
            ]),
            payload.requester || 'Requesting officer',
            payload.location || 'Incident location'
        );
        appendFeed(realtimeFeed, 'Tele-support requested for ' + compactText(payload.incidentNumber || 'active incident'), payload.severity || 'REQUESTED');
    }

    function handleVideoStarted(payload) {
        if (!payload) {
            return;
        }
        if (officerLabel) {
            officerLabel.textContent = participantLabel(payload);
        }
        if (locationLabel) {
            locationLabel.textContent = payload.location || 'Live location';
        }
        appendFeed(realtimeFeed, 'Live video started for ' + (payload.incidentNumber || 'unassigned incident'), 'LIVE');
        appendFeed(sessionFeed, participantLabel(payload) + ' at ' + (payload.location || 'Live'), 'LIVE');

        const participantType = compactText(payload.participantType).toUpperCase();
        if (participantType === 'PUBLIC' && matchesScope(payload)) {
            pendingStationCallId = payload.callId || pendingStationCallId;
            openAlarmVideoPopup(
                'Public emergency live video',
                joinMeta([
                    payload.incidentNumber,
                    payload.stationId ? 'Station live feed' : '',
                    payload.startTime
                ]),
                participantLabel(payload),
                payload.location || 'Public scene'
            );
            return;
        }

        if (participantType === 'OFFICER' && matchesScope(payload)) {
            showAlarm(
                'Officer live video alert',
                'LIVE FIELD VIDEO STARTED FOR THIS EMERGENCY',
                joinMeta([
                    payload.incidentNumber,
                    payload.location,
                    payload.startTime
                ]),
                { profile: 'standard', rate: 0.92, pitch: 1.0 }
            );
            openAlarmVideoPopup(
                'Officer live emergency video',
                joinMeta([
                    payload.incidentNumber,
                    payload.startTime
                ]),
                participantLabel(payload),
                payload.location || 'Incident location'
            );
        }

        if (isTeleSupportSpecialistRole()
                && pendingTeleSupportIncidentId !== null
                && payload.incidentId !== null
                && typeof payload.incidentId !== 'undefined'
                && String(pendingTeleSupportIncidentId) === String(payload.incidentId)) {
            openAlarmVideoPopup(
                'Tele-support live consultation',
                joinMeta([
                    payload.incidentNumber,
                    payload.startTime
                ]),
                participantLabel(payload),
                payload.location || 'Incident location'
            );
        }
    }

    function playAlarmSound(profile) {
        if (!alarmAudioPlayer) {
            alarmAudioPlayer = new Audio(alarmAudioUrl);
            alarmAudioPlayer.preload = 'auto';
        }
        alarmAudioPlayer.pause();
        alarmAudioPlayer.currentTime = 0;
        alarmAudioPlayer.volume = 1;
        alarmAudioPlayer.play().catch(function () {
            playAlarmTone(profile);
        });
    }

    function playAlarmTone(profile) {
        const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
        if (!AudioContextCtor) {
            return;
        }
        if (!alarmAudioContext) {
            alarmAudioContext = new AudioContextCtor();
        }
        if (alarmAudioContext.state === 'suspended') {
            alarmAudioContext.resume().catch(function () {});
        }
        const profiles = {
            standard: { pattern: [0, 280, 560], frequency: 920, peak: 0.12, duration: 0.26, type: 'square' },
            dispatch: { pattern: [0, 240, 480, 720], frequency: 980, peak: 0.16, duration: 0.3, type: 'square' },
            critical: { pattern: [0, 180, 360, 540, 720, 900], frequency: 1140, peak: 0.24, duration: 0.34, type: 'sawtooth' },
            teleSupport: { pattern: [0, 220, 440, 660], frequency: 1040, peak: 0.18, duration: 0.3, type: 'triangle' },
            teleSupportCritical: { pattern: [0, 160, 320, 480, 640, 800], frequency: 1240, peak: 0.26, duration: 0.34, type: 'sawtooth' }
        };
        const selected = profiles[profile] || profiles.standard;
        const pattern = selected.pattern;
        pattern.forEach(function (delay) {
            const oscillator = alarmAudioContext.createOscillator();
            const gain = alarmAudioContext.createGain();
            oscillator.type = selected.type;
            oscillator.frequency.value = selected.frequency;
            gain.gain.value = 0.0001;
            oscillator.connect(gain);
            gain.connect(alarmAudioContext.destination);
            const startAt = alarmAudioContext.currentTime + (delay / 1000);
            oscillator.start(startAt);
            gain.gain.exponentialRampToValueAtTime(selected.peak, startAt + 0.02);
            gain.gain.exponentialRampToValueAtTime(0.0001, startAt + (selected.duration - 0.02));
            oscillator.stop(startAt + selected.duration);
        });
    }

    function speakAlarm(message, options) {
        if (!message || !('speechSynthesis' in window)) {
            return;
        }
        try {
            window.speechSynthesis.cancel();
            const utterance = new SpeechSynthesisUtterance(message);
            utterance.rate = options && options.rate ? options.rate : 0.92;
            utterance.pitch = options && options.pitch ? options.pitch : 0.88;
            utterance.volume = 1;
            window.speechSynthesis.speak(utterance);
        } catch (error) {
            console.warn('Alarm speech failed', error);
        }
    }

    function currentPublisherAudioEnabled() {
        return !streamAudioMode || String(streamAudioMode.value) !== 'false';
    }

    function applyPublisherAudioState(enabled) {
        if (!localStream) {
            return;
        }
        localStream.getAudioTracks().forEach(function (track) {
            track.enabled = enabled;
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

    async function getResponseAudioStream() {
        if (!canPublish || isPublishingLive) {
            return null;
        }
        if (responseAudioStream && responseAudioStream.getTracks().some(function (track) { return track.readyState === 'live'; })) {
            return responseAudioStream;
        }
        responseAudioStream = await navigator.mediaDevices.getUserMedia({
            audio: mediaConstraints.audio,
            video: false
        });
        responseAudioStream.getAudioTracks().forEach(function (track) {
            track.enabled = true;
        });
        return responseAudioStream;
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
            if (event.streams && event.streams[0]) {
                if (remoteVideo) {
                    attachStream(remoteVideo, event.streams[0]);
                }
                if (alarmRemoteVideo) {
                    attachStream(alarmRemoteVideo, event.streams[0]);
                }
                syncRemoteAudioState();
                updateSignalStatus('Remote stream connected');
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
        const url = appContext.signalUrl ? protocol + window.location.host + appContext.signalUrl : protocol + window.location.host + '/signal';
        socket = new WebSocket(url);
        signalReadyPromise = new Promise(function (resolve, reject) {
            function clearPending() {
                signalReadyPromise = null;
            }

            function handleOpen() {
                socket.removeEventListener('error', handleError);
                socket.removeEventListener('close', handleCloseBeforeOpen);
                resolve(socket);
            }

            function handleError() {
                socket.removeEventListener('open', handleOpen);
                socket.removeEventListener('close', handleCloseBeforeOpen);
                clearPending();
                reject(new Error('Signal channel unavailable'));
            }

            function handleCloseBeforeOpen() {
                socket.removeEventListener('open', handleOpen);
                socket.removeEventListener('error', handleError);
                clearPending();
                reject(new Error('Signal channel closed before connection was established'));
            }

            socket.addEventListener('open', handleOpen, { once: true });
            socket.addEventListener('error', handleError, { once: true });
            socket.addEventListener('close', handleCloseBeforeOpen, { once: true });
        });

        socket.addEventListener('open', function () {
            updateSignalStatus('Signal online');
            appendFeed(realtimeFeed, 'Signal channel connected', 'LIVE');
        });

        socket.addEventListener('close', function () {
            signalReadyPromise = null;
            socket = null;
            updateSignalStatus('Signal offline');
            appendFeed(realtimeFeed, 'Signal channel closed', 'OFFLINE');
        });

        socket.addEventListener('message', async function (event) {
            let message;
            try {
                message = JSON.parse(event.data);
            } catch (error) {
                return;
            }

            if (message.type === 'PUBLIC_CALL_CREATED') {
                handlePublicCallAlert(message.payload || {});
                return;
            }

            if (message.type === 'PUBLIC_REPORT_MESSAGE') {
                handlePublicMessageAlert(message.payload || {});
                return;
            }

            if (message.type === 'CALL_ACCEPTED') {
                handleCallAcceptedAlert(message.payload || {});
                return;
            }

            if (message.type === 'TELE_SUPPORT_REQUESTED') {
                handleTeleSupportAlert(message.payload || {});
                return;
            }

            if (message.type === 'VIDEO_STARTED') {
                handleVideoStarted(message.payload || {});
                return;
            }

            if (message.type === 'VIDEO_ENDED') {
                if (remoteVideo && !isPublishingLive) {
                    remoteVideo.srcObject = null;
                }
                if (alarmRemoteVideo && !isPublishingLive) {
                    alarmRemoteVideo.srcObject = null;
                }
                if (!isPublishingLive) {
                    stopStream(responseAudioStream);
                    responseAudioStream = null;
                    if (peerConnection) {
                        peerConnection.close();
                        peerConnection = null;
                    }
                }
                if (pendingTeleSupportIncidentId !== null
                        && message.payload
                        && message.payload.incidentId !== null
                        && typeof message.payload.incidentId !== 'undefined'
                        && String(pendingTeleSupportIncidentId) === String(message.payload.incidentId)) {
                    pendingTeleSupportIncidentId = null;
                }
                appendFeed(realtimeFeed, 'Live video ended', 'ENDED');
                return;
            }

            if (message.type === 'INCIDENT_CREATED') {
                if (!matchesScope(message.payload || {})) {
                    return;
                }
                appendFeed(realtimeFeed, 'New incident ' + (message.payload.incidentNumber || ''), message.payload.severity || 'NEW');
                return;
            }

            if (message.type === 'offer' && !isPublishingLive) {
                if (!shouldAutoAnswerOffer(message)) {
                    return;
                }
                const viewerConnection = createPeerConnection();
                viewerConnection.remoteSessionId = message.senderSessionId || null;
                const replyAudioStream = await getResponseAudioStream();
                if (replyAudioStream) {
                    replyAudioStream.getTracks().forEach(function (track) {
                        const alreadyAdded = viewerConnection.getSenders().some(function (sender) {
                            return sender.track && sender.track.id === track.id;
                        });
                        if (!alreadyAdded) {
                            viewerConnection.addTrack(track, replyAudioStream);
                        }
                    });
                }
                await viewerConnection.setRemoteDescription(new RTCSessionDescription(message.payload));
                const answer = await viewerConnection.createAnswer();
                await viewerConnection.setLocalDescription(answer);
                socket.send(JSON.stringify({
                    type: 'answer',
                    payload: answer,
                    targetSessionId: message.senderSessionId || null
                }));
                return;
            }

            if (message.type === 'answer' && isPublishingLive && peerConnection) {
                peerConnection.remoteSessionId = message.senderSessionId || peerConnection.remoteSessionId || null;
                await peerConnection.setRemoteDescription(new RTCSessionDescription(message.payload));
                updateSignalStatus('Viewer connected');
                return;
            }

            if (message.type === 'ice-candidate' && peerConnection) {
                try {
                    await peerConnection.addIceCandidate(new RTCIceCandidate(message.payload));
                } catch (error) {
                    console.warn('ICE candidate rejected', error);
                }
            }
        });

        return signalReadyPromise;
    }

    async function resolveLocation() {
        return new Promise(function (resolve) {
            if (!navigator.geolocation) {
                resolve({});
                return;
            }
            navigator.geolocation.getCurrentPosition(function (position) {
                resolve({
                    latitude: String(position.coords.latitude),
                    longitude: String(position.coords.longitude),
                    locationLabel: 'GPS field location'
                });
            }, function () {
                resolve({ locationLabel: 'Location permission denied' });
            }, { enableHighAccuracy: true, timeout: 6000 });
        });
    }

    async function startStream() {
        if (!canPublish) {
            appendFeed(realtimeFeed, 'Live publishing is blocked for this role', 'DENIED');
            return;
        }
        await connectSignal();
        const audioEnabled = currentPublisherAudioEnabled();
        const requestedStream = await getPreferredMediaStream(audioEnabled);
        const locationData = await resolveLocation();
        const selectedIncidentId = teleSupportIncidentId && teleSupportIncidentId.value
            ? teleSupportIncidentId.value
            : '';
        const formData = new URLSearchParams(locationData);
        formData.append('audioEnabled', String(audioEnabled));
        if (selectedIncidentId) {
            formData.append('incidentId', selectedIncidentId);
        }
        const startResponse = await window.fetch(appContext.startVideoUrl || '/api/video/sessions/start', {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData
        });

        if (!startResponse.ok) {
            requestedStream.getTracks().forEach(function (track) {
                track.stop();
            });
            appendFeed(realtimeFeed, 'Failed to start video session', 'ERROR');
            return;
        }

        const session = await startResponse.json();
        localStream = requestedStream;
        isPublishingLive = true;
        currentVideoSessionId = session.id;
        if (localVideo) {
            attachStream(localVideo, localStream);
        }
        if (remoteVideo) {
            attachStream(remoteVideo, localStream);
        }
        syncRemoteAudioState();
        if (officerLabel) {
            officerLabel.textContent = appContext.username || 'Control Room Attendant';
        }
        if (locationLabel) {
            locationLabel.textContent = locationData.locationLabel || 'Live location';
        }
        createPeerConnection();
        localStream.getTracks().forEach(function (track) {
            peerConnection.addTrack(track, localStream);
        });
        const offer = await peerConnection.createOffer();
        await peerConnection.setLocalDescription(offer);
        socket.send(JSON.stringify({
            type: 'offer',
            payload: offer,
            incidentId: session.incidentId || (selectedIncidentId || null),
            callId: session.callId || null
        }));
        mediaRecorder = new MediaRecorder(localStream, { mimeType: 'video/webm' });
        recordedChunks = [];
        mediaRecorder.ondataavailable = function (event) {
            if (event.data && event.data.size > 0) {
                recordedChunks.push(event.data);
            }
        };
        mediaRecorder.start(1000);
        updateSignalStatus('Broadcasting live');
    }

    async function uploadRecording(blob) {
        if (!blob || !currentVideoSessionId) {
            return;
        }
        const formData = new FormData();
        formData.append('videoSessionId', String(currentVideoSessionId));
        formData.append('file', blob, 'session-' + currentVideoSessionId + '.webm');
        const response = await window.fetch(appContext.uploadVideoUrl || '/api/video/upload', {
            method: 'POST',
            credentials: 'same-origin',
            body: formData
        });
        if (response.ok) {
            appendFeed(realtimeFeed, 'Recording uploaded for session ' + currentVideoSessionId, 'RECORDED');
        }
    }

    async function endStream() {
        if (mediaRecorder && mediaRecorder.state !== 'inactive') {
            mediaRecorder.stop();
            await new Promise(function (resolve) {
                mediaRecorder.onstop = resolve;
            });
        }

        if (localStream) {
            stopStream(localStream);
            localStream = null;
            if (localVideo) {
                localVideo.srcObject = null;
            }
            if (remoteVideo) {
                remoteVideo.srcObject = null;
            }
        }

        if (peerConnection) {
            peerConnection.close();
            peerConnection = null;
        }
        stopStream(responseAudioStream);
        responseAudioStream = null;

        if (currentVideoSessionId) {
            const endUrlTemplate = appContext.endVideoUrl || '/api/video/sessions/__id__/end';
            await window.fetch(endUrlTemplate.replace('__id__', String(currentVideoSessionId)), {
                method: 'POST',
                credentials: 'same-origin'
            });
        }

        if (recordedChunks.length > 0) {
            const blob = new Blob(recordedChunks, { type: 'video/webm' });
            await uploadRecording(blob);
        } else if (uploadInput && uploadInput.files && uploadInput.files[0] && currentVideoSessionId) {
            await uploadRecording(uploadInput.files[0]);
        }

        recordedChunks = [];
        isPublishingLive = false;
        currentVideoSessionId = null;
        syncRemoteAudioState();
        updateSignalStatus('Stream ended');
    }

    if (startButton) {
        startButton.addEventListener('click', function () {
            startStream().catch(function (error) {
                console.error(error);
                appendFeed(realtimeFeed, 'Unable to start live stream', 'ERROR');
            });
        });
    }

    if (endButton) {
        endButton.addEventListener('click', function () {
            endStream().catch(function (error) {
                console.error(error);
                appendFeed(realtimeFeed, 'Unable to end live stream', 'ERROR');
            });
        });
    }

    if (remoteAudioButton) {
        remoteAudioButton.addEventListener('click', function () {
            remoteAudioMuted = !remoteAudioMuted;
            syncRemoteAudioState();
        });
        syncRemoteAudioState();
    }

    if (streamAudioMode) {
        streamAudioMode.addEventListener('change', function () {
            applyPublisherAudioState(currentPublisherAudioEnabled());
        });
    }

    if (remoteVideo || localVideo) {
        connectSignal().catch(function (error) {
            console.error(error);
            updateSignalStatus('Signal unavailable');
        });
    }

    const incomingCallModalElement = document.getElementById('incomingCallModal');
    const incomingCallModal = incomingCallModalElement ? new bootstrap.Modal(incomingCallModalElement) : null;

    async function submitControlRoomCallAction(url, payload) {
        const response = await window.fetch(url, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload || {})
        });
        const actionPayload = await readResponsePayload(response);
        if (!response.ok) {
            throw new Error(actionPayload.error || 'Unable to update call');
        }
        return actionPayload;
    }

    if (incomingCallModal) {
        incomingCallModal.show();
    }

    document.querySelectorAll('[data-accept-call]').forEach(function (button) {
        button.addEventListener('click', function () {
            const callId = button.getAttribute('data-accept-call');
            const acceptUrlTemplate = appContext.acceptCallUrlTemplate || '/api/control-room/calls/__id__/accept';
            confirmActionPrompt('Accept and route this call?', 'This will route the call to the target station and notify the station team to register the initial incident report.', 'Accept call')
                .then(function (confirmed) {
                    if (!confirmed) {
                        return null;
                    }
                    return submitControlRoomCallAction(acceptUrlTemplate.replace('__id__', String(callId)), {})
                        .then(function () {
                            if (incomingCallModal) {
                                incomingCallModal.hide();
                            }
                            return Swal.fire({
                                icon: 'success',
                                title: 'Call accepted',
                                text: 'The station has been notified and can now register the initial incident report.'
                            });
                        })
                        .then(function () {
                            window.location.reload();
                        });
                })
                .catch(function (error) {
                    if (error) {
                        Swal.fire({ icon: 'error', title: 'Accept failed', text: error.message });
                    }
                });
        });
    });

    document.querySelectorAll('[data-reject-call]').forEach(function (button) {
        button.addEventListener('click', function () {
            const callId = button.getAttribute('data-reject-call');
            const rejectUrlTemplate = appContext.rejectCallUrlTemplate || '/api/control-room/calls/__id__/reject';
            Swal.fire({
                title: 'Reject incoming call',
                input: 'textarea',
                inputLabel: 'Reason',
                inputPlaceholder: 'Optional reason for rejection',
                showCancelButton: true,
                confirmButtonText: 'Reject call'
            }).then(function (result) {
                if (!result.isConfirmed) {
                    return null;
                }
                return submitControlRoomCallAction(rejectUrlTemplate.replace('__id__', String(callId)), {
                    reason: result.value || ''
                }).then(function () {
                    if (incomingCallModal) {
                        incomingCallModal.hide();
                    }
                    return Swal.fire({
                        icon: 'success',
                        title: 'Call rejected',
                        text: 'The call record has been marked as rejected.'
                    });
                }).then(function () {
                    window.location.reload();
                });
            }).catch(function (error) {
                Swal.fire({ icon: 'error', title: 'Reject failed', text: error.message });
            });
        });
    });

    const openCallChatButtons = document.querySelectorAll('[data-open-call-chat]');
    const controlRoomChatForm = document.getElementById('controlRoomChatForm');
    const controlRoomChatFeed = document.getElementById('controlRoomChatFeed');
    const controlRoomChatCallId = document.getElementById('controlRoomChatCallId');
    const controlRoomChatReportNumber = document.getElementById('controlRoomChatReportNumber');
    const controlRoomChatMessage = document.getElementById('controlRoomChatMessage');

    async function loadControlRoomChat(callId) {
        renderLoadingSkeleton(controlRoomChatFeed, 3);
        const response = await window.fetch('/api/control-room/calls/' + callId + '/messages', {
            method: 'GET',
            credentials: 'same-origin'
        });
        const payload = await readResponsePayload(response);
        if (!response.ok) {
            throw new Error(payload.error || 'Unable to load messages');
        }
        renderListRows(controlRoomChatFeed, payload, 'No messages yet.', function (item) {
            return createListRowElement(item.senderType || 'CONTROL_ROOM', item.message || '', item.createdAt || '');
        });
    }

    openCallChatButtons.forEach(function (button) {
        button.addEventListener('click', function () {
            const callId = button.getAttribute('data-open-call-chat');
            const reportNumber = button.getAttribute('data-call-report') || '';
            if (controlRoomChatCallId) {
                controlRoomChatCallId.value = callId;
            }
            if (controlRoomChatReportNumber) {
                controlRoomChatReportNumber.textContent = reportNumber;
            }
            loadControlRoomChat(callId).catch(function (error) {
                Swal.fire({ icon: 'error', title: 'Chat load failed', text: error.message });
            });
            const modalElement = document.getElementById('callChatModal');
            if (modalElement) {
                new bootstrap.Modal(modalElement).show();
            }
        });
    });

    if (controlRoomChatForm) {
        controlRoomChatForm.addEventListener('submit', function (event) {
            event.preventDefault();
            const callId = controlRoomChatCallId.value;
            window.fetch('/api/control-room/calls/' + callId + '/messages', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: controlRoomChatMessage.value })
            }).then(async function (response) {
                const payload = await readResponsePayload(response);
                if (!response.ok) {
                    throw new Error(payload.error || 'Unable to send message');
                }
                controlRoomChatMessage.value = '';
                return loadControlRoomChat(callId);
            }).catch(function (error) {
                Swal.fire({ icon: 'error', title: 'Message send failed', text: error.message });
            });
        });
    }
});
