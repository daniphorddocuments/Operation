package com.daniphord.mahanga.Service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SystemDocumentationService {

    private final PdfBrandingService pdfBrandingService;

    public SystemDocumentationService(PdfBrandingService pdfBrandingService) {
        this.pdfBrandingService = pdfBrandingService;
    }

    public List<Map<String, String>> adminDocuments() {
        return List.of(
                Map.of(
                        "key", "system-architecture",
                        "title", "FROMS System Architecture Document",
                        "description", "End-to-end platform design covering frontend, backend, database, workflows, security, and Python AI sidecar integration."
                ),
                Map.of(
                        "key", "system-design-document",
                        "title", "FROMS System Design Document",
                        "description", "Detailed system design for modules, user interfaces, workflows, data movement, and implementation structure."
                ),
                Map.of(
                        "key", "system-requirements-specification",
                        "title", "FROMS System Requirements Specification",
                        "description", "Formal specification of users, modules, workflows, security controls, performance expectations, and presentation-critical requirements."
                ),
                Map.of(
                        "key", "disaster-recovery-plan",
                        "title", "FROMS Disaster Recovery Plan",
                        "description", "Administrative recovery document covering incident classification, backup policy, RTO/RPO targets, system restoration, and command continuity."
                ),
                Map.of(
                        "key", "disaster-recovery-runbook",
                        "title", "FROMS Disaster Recovery Runbook",
                        "description", "Step-by-step operational runbook for outage response, database restore validation, service failover, communication, and post-recovery verification."
                )
        );
    }

    public byte[] generatePdf(String key, String lang) {
        return switch (key) {
            case "system-architecture" -> pdfBrandingService.generatePdf(title(key, lang), architectureBody(lang), lang);
            case "system-design-document" -> pdfBrandingService.generatePdf(title(key, lang), designBody(lang), lang);
            case "system-requirements-specification" -> pdfBrandingService.generatePdf(title(key, lang), srsBody(lang), lang);
            case "disaster-recovery-plan" -> pdfBrandingService.generatePdf(title(key, lang), disasterRecoveryPlanBody(lang), lang);
            case "disaster-recovery-runbook" -> pdfBrandingService.generatePdf(title(key, lang), disasterRecoveryRunbookBody(lang), lang);
            default -> throw new IllegalArgumentException("Unknown document key");
        };
    }

    private String architectureBody(String lang) {
        if ("sw".equalsIgnoreCase(lang)) {
            return """
                <div class="section-card">
                    <h2>1. Madhumuni ya Hati</h2>
                    <p>Hati hii ya usanifu inaeleza muundo wa kiufundi wa Mfumo wa Usimamizi wa Operesheni za Jeshi la Zimamoto na Uokoaji (FROMS). Hati imeandaliwa kwa uwasilishaji wa viongozi, majaji wa kiufundi, na mapitio ya utekelezaji. Inaeleza namna mfumo unavyosaidia upokeaji wa taarifa za umma, uelekezaji wa chumba cha udhibiti, dashibodi za uendeshaji, mtiririko wa uchunguzi wa moto, udhibiti wa usalama, utayari wa ukaguzi, utoaji wa ripoti, na usaidizi wa maamuzi unaotegemea AI.</p>
                </div>
                <div class="section-card">
                    <h2>2. Muhtasari wa Usanifu</h2>
                    <p>FROMS umetengenezwa kama jukwaa la operesheni zenye moduli nyingi. Mfumo mkuu wa miamala unatumia Java Spring Boot kwa mtiririko salama wa biashara, udhibiti wa upatikanaji, uandaaji wa ripoti, na usimamizi wa data. Miingiliano ya watumiaji inatumia violezo vya Thymeleaf, mpangilio wa Bootstrap, na JavaScript kwa dashibodi shirikishi, taarifa za umma, mazungumzo, na uratibu wa video ya moja kwa moja. Huduma ya pembeni ya Python imeunganishwa kwa mapendekezo ya AI na uchambuzi wa ukaguzi wa mfumo.</p>
                </div>
                <div class="section-card">
                    <h2>3. Usanifu wa Kimantiki</h2>
                    <table class="diagram-grid">
                        <tr>
                            <td class="diagram-cell">Tabaka la Ufikiaji wa Umma<br><span class="muted">Taarifa za dharura, ufuatiliaji wa kesi, mazungumzo, video ya umma</span></td>
                            <td class="diagram-cell">Tabaka la Dashibodi za Maafisa<br><span class="muted">Dashibodi za wajibu, chumba cha udhibiti, uchunguzi, zana za usimamizi</span></td>
                        </tr>
                        <tr>
                            <td class="diagram-cell">Tabaka la Programu<br><span class="muted">Controllers za Spring, services, workflows, RBAC, notifications, PDF generation</span></td>
                            <td class="diagram-cell">Tabaka la Akili Tengefu<br><span class="muted">Huduma ya Python kwa mapendekezo na uchambuzi wa ukaguzi</span></td>
                        </tr>
                        <tr>
                            <td class="diagram-cell">Tabaka la Data<br><span class="muted">Watumiaji, matukio, jiografia, uchunguzi, audit logs, viambatanisho, video sessions</span></td>
                            <td class="diagram-cell">Tabaka la Vielelezo<br><span class="muted">Hifadhi ya nyaraka, picha, video na PDF zinazozalishwa</span></td>
                        </tr>
                    </table>
                </div>
                <div class="section-card">
                    <h2>4. Muundo wa Mwonekano</h2>
                    <p>Tabaka la uwasilishaji limepangwa kuwa landing page, login, public emergency, role dashboard, operations dashboard, na control room dashboard. Muundo unasisitiza mpangilio wazi wa mwonekano, usomaji wa kiwango cha uongozi, ulinganifu kwa vifaa tofauti, na upatikanaji wa moduli kwa mujibu wa wajibu. Rasilimali za pamoja za UI hutoa topbar, mfumo wa maandishi, mfumo wa rangi, modals, udhibiti wa kutotumika, na widgets za operesheni hai.</p>
                </div>
                <div class="section-card">
                    <h2>5. Muundo wa Backend</h2>
                    <p>Backend inafuata usanifu wa tabaka za huduma. Controllers hutoa endpoints zenye ulinzi wa wajibu, services zinatekeleza sheria za mtiririko na uthibitishaji, repositories hutoa ufikiaji wa hifadhi ya data, na audit services huhifadhi ushahidi wa uwajibikaji. Backend inasimamia usalama, mfuatano wa workflow, uelekezaji wa kituo, uandaaji wa ripoti, na muhtasari wa dashibodi.</p>
                </div>
                <div class="section-card">
                    <h2>6. Usanifu wa Usalama</h2>
                    <p>Usalama ni hitaji kuu la kiutendaji. Mfumo unatekeleza udhibiti wa upatikanaji kwa wajibu, kufunga akaunti baada ya majaribio ya kuingia yasiyo sahihi, kumalizika kwa session kutokana na kutotumika, audit logging, na mwonekano wa data uliowekewa mipaka. Mifumo ya umma imetengwa na mifumo ya maafisa, na mwonekano wa control room umewekewa kituo husika pale inapohitajika.</p>
                </div>
                <div class="section-card">
                    <h2>7. Usanifu wa Data</h2>
                    <p>Muundo wa data unahifadhi seti za taarifa zilizounganishwa lakini zenye kazi tofauti. Jedwali muhimu ni pamoja na users, regions, districts, stations, incidents, emergency calls, investigations, investigation logs, attachments, user notifications, audit logs, na video sessions.</p>
                </div>
                <div class="section-card">
                    <h2>8. Usanifu wa Muingiliano</h2>
                    <p>FROMS huunganisha moduli zake kupitia uratibu wa kiwango cha huduma. Taarifa za umma huenda control room. Hatua za control room huongeza mwonekano kwenye dashibodi za operesheni. Uchunguzi hutumia data ya matukio na huzalisha ushahidi wa idhini. Mfumo wa PDF huzalisha nyaraka rasmi za uwasilishaji. Huduma ya Python hutoa mapendekezo na uchambuzi bila kuchukua nafasi ya mfumo mkuu wa miamala.</p>
                </div>
                <div class="section-card">
                    <h2>9. Mtazamo wa Utekelezaji</h2>
                    <p>Utekelezaji uliopendekezwa unatumia seva salama ya Spring Boot, hifadhidata ya uhusiano, hifadhi ya faili kwa viambatanisho na video, na huduma ya Python ya ndani kwa huduma za akili. TLS, nakala za hifadhidata, uhifadhi wa audit log, na upimaji wa ratiba vinapaswa kuwa sehemu ya udhibiti wa uzalishaji.</p>
                </div>
                """;
        }
        return """
                <div class="section-card">
                    <h2>1. Document Purpose</h2>
                    <p>This architecture document defines the technical structure of the Fire and Rescue Force Operation Management System (FROMS). The document is prepared for executive presentation, technical judges, and implementation review. It describes how the platform supports public incident intake, control room routing, operational dashboards, fire investigation workflow, security enforcement, audit readiness, reporting, and AI-assisted decision support.</p>
                </div>
                <div class="section-card">
                    <h2>2. Architecture Summary</h2>
                    <p>FROMS is implemented as a multi-module mission operations platform. The primary transactional platform uses Java Spring Boot for secure business workflows, access control, reporting, and persistent data management. The user interfaces use Thymeleaf templates, Bootstrap-based layouts, and JavaScript for interactive dashboards, public reporting, chat, and live video coordination. A Python sidecar service is connected for heuristic AI recommendations and system audit intelligence.</p>
                </div>
                <div class="section-card">
                    <h2>3. Logical Architecture</h2>
                    <table class="diagram-grid">
                        <tr>
                            <td class="diagram-cell">Public Access Layer<br><span class="muted">Emergency reporting, case tracking, chat, public video</span></td>
                            <td class="diagram-cell">Officer Dashboard Layer<br><span class="muted">Role dashboards, control room, investigations, admin tools</span></td>
                        </tr>
                        <tr>
                            <td class="diagram-cell">Application Layer<br><span class="muted">Spring controllers, services, workflows, RBAC, notifications, PDF generation</span></td>
                            <td class="diagram-cell">Intelligence Layer<br><span class="muted">Python AI sidecar for recommendations and audit heuristics</span></td>
                        </tr>
                        <tr>
                            <td class="diagram-cell">Data Layer<br><span class="muted">Users, incidents, geography, investigations, audit logs, attachments, video sessions</span></td>
                            <td class="diagram-cell">Evidence Layer<br><span class="muted">Document, image, video, and generated PDF storage</span></td>
                        </tr>
                    </table>
                </div>
                <div class="section-card">
                    <h2>4. Frontend Design</h2>
                    <p>The presentation layer is organized into landing, login, public emergency, role dashboard, operations dashboard, and control room dashboard experiences. The design emphasizes clear visual hierarchy, command-level readability, responsive layout behavior, and role-scoped access. Shared UI resources provide the consistent topbar, typography, color system, modal handling, inactivity management, and live operational widgets.</p>
                    <ul>
                        <li>Landing page: government presentation surface and public entry point.</li>
                        <li>Login page: secure officer access with language switching.</li>
                        <li>Role dashboards: expose only permitted modules and operational data.</li>
                        <li>Public emergency page: guided citizen reporting, case-room chat, and live video escalation.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>5. Backend Design</h2>
                    <p>The backend follows a layered service architecture. Controllers expose role-protected endpoints, services enforce workflow and validation rules, repositories provide persistent storage access, and audit services record accountability events. The backend is responsible for security policy enforcement, workflow progression, station-scoped routing, report generation, and dashboard summarization.</p>
                    <ul>
                        <li>Authentication and session control.</li>
                        <li>Incident registration and operations management.</li>
                        <li>Geography registry for region, district, and station routing.</li>
                        <li>Fire investigation hierarchical approval workflow.</li>
                        <li>System verification and scheduled six-month audit reporting.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>6. Security Architecture</h2>
                    <p>Security is designed as a first-class operational requirement. The platform enforces role-based access control, lockout after repeated failed login attempts, inactivity timeout, audit logging, and scoped data visibility. Public workflows are isolated from protected officer workflows, and control-room visibility is limited to the assigned station where required. Password strength indicators are stored to support administrative review and periodic testing.</p>
                    <ul>
                        <li>Five-attempt account lockout with administrative unlock capability.</li>
                        <li>Five-minute inactivity timeout with follow-up response window.</li>
                        <li>Control room routing limited to selected nearby fire station.</li>
                        <li>Investigation approvals restricted to designated workflow levels.</li>
                        <li>Security headers, CSRF handling, and audit evidence generation.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>7. Data Architecture</h2>
                    <p>The persistence model stores operationally distinct but connected datasets. Core tables include users, regions, districts, stations, incidents, emergency calls, investigations, investigation logs, attachments, user notifications, audit logs, and video sessions. The data model is normalized around operational scope so that reporting, workflow approval, and station-based access can be enforced consistently.</p>
                </div>
                <div class="section-card">
                    <h2>8. Integration Architecture</h2>
                    <p>FROMS integrates its operational modules through service-level coordination. Public reports feed the control room. Control room actions create visibility for operations dashboards. Investigations consume incident data and produce approval evidence. The PDF subsystem produces formal presentation documents. The Python AI sidecar supports recommendation logic and system audit heuristics without replacing the core transactional platform.</p>
                </div>
                <div class="section-card">
                    <h2>9. Deployment View</h2>
                    <p>The recommended deployment model uses a secured Spring Boot application server, a relational database, controlled filesystem storage for attachments and video artifacts, and a local or internal-network Python sidecar for intelligence services. TLS termination, database backup, audit-log retention, and scheduled testing should be included in production deployment controls.</p>
                </div>
                <div class="section-card">
                    <h2>10. Presentation Readiness</h2>
                    <p>This architecture is presentation-ready for government and international technical evaluation. It demonstrates operational discipline, role-sensitive workflow design, security governance, extensibility through Python intelligence services, and document generation suitable for formal review.</p>
                </div>
                """;
    }

    private String srsBody(String lang) {
        if ("sw".equalsIgnoreCase(lang)) {
            return """
                <div class="section-card">
                    <h2>1. Madhumuni</h2>
                    <p>Hati hii ya System Requirements Specification inaeleza mahitaji ya kiutendaji, kiusalama, na ya nyaraka kwa Mfumo wa Usimamizi wa Operesheni za Jeshi la Zimamoto na Uokoaji (FROMS). Hati inalenga utekelezaji wa kiufundi, uthibitishaji wa viongozi, na tathmini ya nje.</p>
                </div>
                <div class="section-card">
                    <h2>2. Upeo wa Mfumo</h2>
                    <p>FROMS unasaidia operesheni za zimamoto na uokoaji kutoka makao makuu hadi ngazi ya kituo. Mfumo hutoa usajili wa matukio, mapokezi ya control room, uelekezaji wa kituo, uratibu wa moja kwa moja, uchunguzi, udhibiti wa kiutawala, na maarifa yanayosaidiwa na AI.</p>
                </div>
                <div class="section-card">
                    <h2>3. Makundi ya Watumiaji</h2>
                    <ul>
                        <li>Kamishna Jenerali wa Zimamoto.</li>
                        <li>Kamishna wa Operesheni.</li>
                        <li>Wakuu wa idara na maafisa wa operesheni.</li>
                        <li>Watumiaji wa mikoa, wilaya, vituo na control room.</li>
                        <li>Wajibu wa uchunguzi kama DIO, DFO, RIO, RFO, na FI-HOD.</li>
                        <li>System administrator na watoa taarifa wa umma.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>4. Mahitaji ya Kiutendaji</h2>
                    <ul>
                        <li>Mfumo utasajili na kusimamia matukio kwa severity, source, response time, geography, na status.</li>
                        <li>Mfumo utaruhusu umma kutoa taarifa za dharura kwa kutumia region, district, ward, na nearby station.</li>
                        <li>Mfumo utaelekeza taarifa ya umma kwa control room ya kituo kilichochaguliwa tu.</li>
                        <li>Mfumo utazalisha report number kwa kila taarifa ya umma na kutoa public case tracking.</li>
                        <li>Mfumo utawezesha chat kati ya reporter na control room pamoja na live video escalation.</li>
                        <li>Mfumo utatoa role-based dashboards zinazoonyesha moduli na data zinazohitajika na kila wajibu.</li>
                        <li>Mfumo utatekeleza mtiririko kamili wa idhini za uchunguzi wa moto kutoka DIO hadi CGF.</li>
                        <li>Mfumo utazalisha PDF zinazopakuliwa kwa investigations, system tests, user manuals, na nyaraka za usimamizi.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>5. Mahitaji ya Usalama</h2>
                    <ul>
                        <li>Mfumo utaifunga akaunti baada ya majaribio matano yasiyo sahihi ya nenosiri.</li>
                        <li>Mfumo utaonyesha idadi ya majaribio yaliyobaki kabla ya account lock.</li>
                        <li>Mfumo utamruhusu msimamizi kufungua akaunti iliyofungwa.</li>
                        <li>Mfumo utamaliza session isiyotumika baada ya dakika tano na kumtoa mtumiaji baada ya muda wa onyo uliowekwa.</li>
                        <li>Mfumo utahifadhi audit log kwa authentication, hatua za kiutawala, na verification events.</li>
                        <li>Mfumo utatambua weak passwords wakati wa system testing ya kiutawala.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>6. Mahitaji ya Utendaji</h2>
                    <p>Mfumo utatoa mwitikio wa haraka wa dashibodi, kudumisha takwimu za response time, na kusaidia ripoti za verification kila baada ya miezi sita. Moduli ya verification itapima representative repository and data-access timings na kujumuisha matokeo hayo kwenye audit report.</p>
                </div>
                <div class="section-card">
                    <h2>7. Mahitaji ya Kiolesura</h2>
                    <p>Kiolesura cha mtumiaji kitasaidia desktop na mobile, mpangilio wa uwasilishaji wa serikali, paneli za dashibodi kwa mujibu wa wajibu, dropdown navigation kwa public reporting geography, na ufikiaji wa nyaraka na verification kwa admin.</p>
                </div>
                <div class="section-card">
                    <h2>8. Mahitaji ya Nyaraka</h2>
                    <p>Mfumo utaonyesha nyaraka za kiutawala zinazopakuliwa ikiwa ni pamoja na System Requirements Specification, System Design Document, na System Architecture Document. PDF zinazozalishwa zitatumia mpangilio rasmi wa serikali, maandishi ya taasisi yaliyo katikati, na branding ya taifa inayofaa kwa uwasilishaji rasmi.</p>
                </div>
                """;
        }
        return """
                <div class="section-card">
                    <h2>1. Purpose</h2>
                    <p>This System Requirements Specification defines the operational, functional, security, and documentation requirements for the Fire and Rescue Force Operation Management System (FROMS). The document is intended for technical implementation, executive validation, and external judging review.</p>
                </div>
                <div class="section-card">
                    <h2>2. System Scope</h2>
                    <p>FROMS supports fire and rescue command operations from national headquarters to station level. It provides incident registration, control room intake, station-based routing, live coordination, investigation workflow, administrative control, and AI-assisted operational insight.</p>
                </div>
                <div class="section-card">
                    <h2>3. User Classes</h2>
                    <ul>
                        <li>Commissioner General of Fire (CGF).</li>
                        <li>Commissioner of Operations.</li>
                        <li>Department heads and operational officers.</li>
                        <li>Regional, district, station, and control-room users.</li>
                        <li>Investigation roles including DIO, DFO, RIO, RFO, and FI-HOD.</li>
                        <li>System administrator and public emergency reporters.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>4. Functional Requirements</h2>
                    <ul>
                        <li>The system shall register and manage incidents with severity, source, response time, geography, and status.</li>
                        <li>The system shall allow the public to report emergencies using region, district, ward, and nearby station selections.</li>
                        <li>The system shall route public reports only to the selected nearby station control room.</li>
                        <li>The system shall generate a report number for each public report and provide public case tracking.</li>
                        <li>The system shall support reporter-to-control-room chat and live video escalation.</li>
                        <li>The system shall provide role-based dashboards exposing only the modules and data required by each user role.</li>
                        <li>The system shall implement the full fire investigation approval workflow from DIO to CGF.</li>
                        <li>The system shall generate downloadable PDF reports for investigations, system tests, and administrative documents.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>5. Security Requirements</h2>
                    <ul>
                        <li>The system shall lock a user account after five incorrect password attempts.</li>
                        <li>The system shall display the number of remaining login attempts before account lock.</li>
                        <li>The system shall allow an administrator to unlock a locked account.</li>
                        <li>The system shall expire inactive sessions after five minutes and log users out after the configured response window.</li>
                        <li>The system shall keep an audit log for authentication, administrative actions, and verification events.</li>
                        <li>The system shall identify weak passwords during administrative system testing.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>6. Performance Requirements</h2>
                    <p>The system shall provide timely dashboard responses, maintain measurable incident response statistics, and support scheduled six-month verification reporting. The verification module shall measure representative repository and data-access timings and include those observations in the generated audit report.</p>
                </div>
                <div class="section-card">
                    <h2>7. Interface Requirements</h2>
                    <p>The user interface shall support desktop and mobile layouts, government-grade presentation formatting, role-specific dashboard panels, dropdown-based navigation for public reporting geography, and administrative access to verification and documentation outputs.</p>
                </div>
                <div class="section-card">
                    <h2>8. Documentation Requirements</h2>
                    <p>The system shall expose downloadable administrative documents including the System Requirements Specification, the System Design Document, and the System Architecture Document. Generated PDF reports shall use official government heading format, centered institutional text, and national branding suitable for formal presentation.</p>
                </div>
                <div class="section-card">
                    <h2>9. Future Extension Requirements</h2>
                    <p>The architecture shall remain extensible for Python-based intelligence services including image analysis, speech processing, predictive analytics, and advanced risk modeling without undermining the reliability of the main transactional platform.</p>
                </div>
                """;
    }

    private String designBody(String lang) {
        if ("sw".equalsIgnoreCase(lang)) {
            return """
                <div class="section-card">
                    <h2>1. Madhumuni ya Hati ya Muundo</h2>
                    <p>Hati hii ya muundo wa mfumo inaeleza namna FROMS imegawanywa katika moduli, skrini, huduma, na mtiririko wa data wa kila siku. Inalenga kutoa mwongozo wa utekelezaji, matengenezo, na uwasilishaji wa suluhisho kwa kiwango cha kiufundi na kiutawala.</p>
                </div>
                <div class="section-card">
                    <h2>2. Muundo wa Moduli</h2>
                    <ul>
                        <li>Landing na login kwa uwasilishaji wa serikali na ufikiaji salama wa watumishi.</li>
                        <li>Public emergency reporting kwa taarifa, case tracking, chat, na live video kutoka eneo la tukio.</li>
                        <li>Role dashboard kwa uwasilishaji wa moduli kulingana na wajibu wa mtumiaji.</li>
                        <li>Control room kwa mapokezi ya taarifa, routing, na mawasiliano ya haraka.</li>
                        <li>Investigation workflow kwa uwasilishaji, mapitio, idhini, na PDF za uchunguzi.</li>
                        <li>Administrative modules kwa user management, geography, documentation, na system verification.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>3. Muundo wa Kiolesura</h2>
                    <p>Muundo wa frontend unategemea Thymeleaf, Bootstrap, na app-ui.js/app-ui.css za pamoja. Topbar, sidebar, modals, tables, KPI cards, na action strips hutumika kwa uthabiti katika dashibodi zote. Kiolesura kinazingatia ufikiaji wa role-specific modules, anchor navigation ya sidebar, na ulinganifu wa desktop/mobile.</p>
                </div>
                <div class="section-card">
                    <h2>4. Muundo wa Huduma za Backend</h2>
                    <p>Controllers hushughulikia routing ya HTTP, services hutoa sheria za biashara, repositories hutoa data access, na util classes hushughulikia validation, roles, na request helpers. Mgawanyo huu unaruhusu kila module kuboreshwa bila kuvunja moduli nyingine.</p>
                </div>
                <div class="section-card">
                    <h2>5. Mtiririko wa Data</h2>
                    <p>Taarifa ya umma huingizwa kupitia public reporting module, huthibitishwa, kisha kupelekwa kwenye control room ya kituo husika. Kutoka hapo matukio huonekana katika dashboards za operesheni, uchunguzi hutumia data hiyo hiyo, na ripoti rasmi hutengenezwa kwa PDF. Notification na audit logging hufuatilia matukio ya kiutawala na kiusalama katika kila hatua.</p>
                </div>
                <div class="section-card">
                    <h2>6. Muundo wa Usalama na Udhibiti</h2>
                    <p>Role-based access control, inactivity timeout, account lockout, audit trails, na filtering ya data kwa station/region/district ni sehemu ya muundo wa mfumo. Muundo huu unahakikisha kuwa kila skrini na endpoint zinaonesha data sahihi kwa mtumiaji sahihi.</p>
                </div>
                <div class="section-card">
                    <h2>7. Muundo wa Upanuzi</h2>
                    <p>Huduma ya Python AI imeunganishwa kama sidecar ili mfumo uweze kupokea mapendekezo ya operesheni na uchambuzi wa system audit bila kubadilisha tabia ya mfumo mkuu wa Spring Boot. Mfumo pia umeandaliwa kupokea nyaraka zaidi, ripoti zaidi, na moduli mpya za command support.</p>
                </div>
                """;
        }
        return """
                <div class="section-card">
                    <h2>1. Design Purpose</h2>
                    <p>This system design document explains how FROMS is organized into modules, interfaces, backend services, and operational data flows. It is intended to guide implementation, maintenance, presentation, and future enhancement work at both technical and administrative levels.</p>
                </div>
                <div class="section-card">
                    <h2>2. Module Design</h2>
                    <ul>
                        <li>Landing and login modules for government presentation and secure officer access.</li>
                        <li>Public emergency reporting for incident intake, case tracking, chat, and live scene video.</li>
                        <li>Role dashboard design for role-filtered modules and operational visibility.</li>
                        <li>Control room design for intake, routing, communication, and field coordination.</li>
                        <li>Investigation workflow design for submission, review, approval, denial, and PDF output.</li>
                        <li>Administrative design for user management, geography, documentation, and system verification.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>3. User Interface Design</h2>
                    <p>The frontend is built on Thymeleaf templates, Bootstrap layout primitives, and shared UI assets in app-ui.js and app-ui.css. The design uses a consistent topbar, sidebar navigation, modal dialogs, KPI cards, action strips, and responsive tables across the dashboard experience. Sidebar anchors and section-based layouts are intended to let administrators move quickly between documentation, user management, geography, and system testing.</p>
                </div>
                <div class="section-card">
                    <h2>4. Backend Service Design</h2>
                    <p>The backend follows a controller-service-repository pattern. Controllers manage protected HTTP routes, services implement business rules, repositories handle persistence, and utility classes support validation, role normalization, password strength review, and request/IP tracking. This separation keeps the operational modules maintainable and testable.</p>
                </div>
                <div class="section-card">
                    <h2>5. Data Flow Design</h2>
                    <p>Public reports enter through the public emergency module, are validated, then routed to the appropriate station control room. Those reports feed operational dashboards, investigation workflows, and report generation. Notifications and audit logging are generated alongside these flows so that privileged actions and security events remain traceable.</p>
                </div>
                <div class="section-card">
                    <h2>6. Security and Control Design</h2>
                    <p>Role-based access control, inactivity timeout, account lockout, audit trails, and geography-scoped filtering are integral to the design. Each screen and endpoint is expected to expose only the modules and data relevant to the authenticated role and operational scope.</p>
                </div>
                <div class="section-card">
                    <h2>7. Extension Design</h2>
                    <p>The Python AI sidecar is designed as an auxiliary service for incident recommendations and system-audit analysis so the main Spring Boot platform remains the authoritative transactional system. The document subsystem is also structured to accommodate additional administrative documents and PDF outputs as the platform grows.</p>
                </div>
                """;
    }

    private String disasterRecoveryPlanBody(String lang) {
        if ("sw".equalsIgnoreCase(lang)) {
            return """
                <div class="section-card">
                    <h2>1. Madhumuni</h2>
                    <p>Mpango huu wa uokoaji wa majanga unaeleza namna FROMS itakavyorejeshwa baada ya hitilafu kubwa ya mfumo, kupotea kwa hifadhidata, kuharibika kwa seva, au tukio la usalama. Lengo ni kulinda mwendelezo wa operesheni za zimamoto na uokoaji pamoja na ushahidi wa kiutawala.</p>
                </div>
                <div class="section-card">
                    <h2>2. Huduma Muhimu</h2>
                    <ul>
                        <li>Uthibitishaji wa watumiaji, dashibodi za wajibu, na control room.</li>
                        <li>Usajili wa matukio, taarifa za dharura za umma, chat, na live video sessions.</li>
                        <li>Hifadhidata ya users, incidents, emergency calls, investigations, audit logs, na attachments.</li>
                        <li>Uzalishaji wa PDF, nyaraka za admin, na ripoti za verification.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>3. Malengo ya Urejeshaji</h2>
                    <ul>
                        <li>Recovery Time Objective (RTO): ndani ya saa 4 kwa huduma muhimu.</li>
                        <li>Recovery Point Objective (RPO): si zaidi ya dakika 15 za data kwa hifadhidata kuu.</li>
                        <li>Audit evidence, user records, na incident records lazima zihifadhiwe wakati wa recovery.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>4. Mkakati wa Nakala Rudufu</h2>
                    <p>Nakala za hifadhidata zichukuliwe kwa ratiba ya kawaida, zihifadhiwe nje ya seva kuu, na zithibitishwe kwa majaribio ya kurejesha. Faili za viambatanisho, video, na PDF zihifadhiwe kwenye hifadhi inayoweza kurejeshwa kwa point-in-time au versioned backup.</p>
                </div>
                <div class="section-card">
                    <h2>5. Mlolongo wa Majibu</h2>
                    <ol class="ui-manual-list">
                        <li>Tambua aina ya tukio na kiwango cha athari.</li>
                        <li>Linda ushahidi wa audit na usimamamishe mabadiliko yasiyo ya lazima.</li>
                        <li>Hamisha huduma kwenye mazingira ya uokoaji au anza urejeshaji wa nodi kuu.</li>
                        <li>Rejesha hifadhidata, kisha thibitisha users, incidents, reports, na role access.</li>
                        <li>Wasiliana kwa uongozi, control room, na wasimamizi wa mifumo hadi huduma zirudi kawaida.</li>
                    </ol>
                </div>
                <div class="section-card">
                    <h2>6. Uthibitishaji Baada ya Urejeshaji</h2>
                    <p>Baada ya recovery, system tests za login, dashboard access, public reporting, user management, na document generation zifanywe. Ripoti ya tukio, muda wa recovery, na mapungufu ya udhibiti yaandikwe kwenye audit trail.</p>
                </div>
                """;
        }
        return """
                <div class="section-card">
                    <h2>1. Purpose</h2>
                    <p>This disaster recovery plan defines how FROMS is restored after major application failure, database loss, infrastructure disruption, or a severe security event. The objective is to preserve fire and rescue operational continuity as well as administrative evidence.</p>
                </div>
                <div class="section-card">
                    <h2>2. Critical Services</h2>
                    <ul>
                        <li>User authentication, role dashboards, and control room access.</li>
                        <li>Incident registration, public emergency intake, chat, and live video sessions.</li>
                        <li>Persistent storage for users, incidents, emergency calls, investigations, audit logs, and attachments.</li>
                        <li>PDF generation, administrative documents, and verification reporting.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>3. Recovery Objectives</h2>
                    <ul>
                        <li>Recovery Time Objective (RTO): restore critical services within 4 hours.</li>
                        <li>Recovery Point Objective (RPO): no more than 15 minutes of data loss on the primary database.</li>
                        <li>Audit evidence, user records, and incident records must remain recoverable during restoration.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>4. Backup Strategy</h2>
                    <p>Database backups must run on schedule, remain stored outside the primary runtime host, and be validated through restore drills. Attachments, video artifacts, and generated PDFs should be stored in recoverable versioned storage or point-in-time backup media.</p>
                </div>
                <div class="section-card">
                    <h2>5. Response Sequence</h2>
                    <ol class="ui-manual-list">
                        <li>Classify the outage or breach and determine operational severity.</li>
                        <li>Preserve audit evidence and freeze non-essential changes.</li>
                        <li>Fail over to recovery infrastructure or rebuild the primary node.</li>
                        <li>Restore the database, then validate users, incidents, reports, and role-based access.</li>
                        <li>Communicate recovery status to command leadership, control room teams, and administrators until service is normalized.</li>
                    </ol>
                </div>
                <div class="section-card">
                    <h2>6. Post-Recovery Validation</h2>
                    <p>After restoration, execute login, dashboard access, public reporting, user management, and documentation generation checks. Record the incident timeline, restoration duration, and any failed controls in the audit trail and follow-up review.</p>
                </div>
                """;
    }

    private String disasterRecoveryRunbookBody(String lang) {
        if ("sw".equalsIgnoreCase(lang)) {
            return """
                <div class="section-card">
                    <h2>1. Madhumuni ya Runbook</h2>
                    <p>Runbook hii inatoa hatua za moja kwa moja za kiutendaji kwa timu ya admin wakati FROMS inahitaji kurejeshwa kwa haraka baada ya hitilafu au tukio la usalama.</p>
                </div>
                <div class="section-card">
                    <h2>2. Hatua za Awali</h2>
                    <ul>
                        <li>Thibitisha alert kutoka monitoring, logs, au taarifa ya mtumiaji.</li>
                        <li>Amua kama hitilafu ni ya application, database, storage, au network.</li>
                        <li>Zuia deployment mpya hadi tathmini ya awali ikamilike.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>3. Urejeshaji wa Hifadhidata</h2>
                    <ol class="ui-manual-list">
                        <li>Chagua backup ya mwisho iliyo verified.</li>
                        <li>Rejesha schema na data kwenye mazingira ya recovery.</li>
                        <li>Kagua users, stations, incidents, emergency calls, na audit logs kabla ya kuruhusu traffic.</li>
                    </ol>
                </div>
                <div class="section-card">
                    <h2>4. Urejeshaji wa Huduma</h2>
                    <ul>
                        <li>Washa Spring Boot application kwa config salama ya environment.</li>
                        <li>Thibitisha muunganisho wa database, storage directories, na Python AI sidecar.</li>
                        <li>Jaribu endpoints za `/login`, `/dashboard`, `/api/public/reports`, na `/api/admin/documents`.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>5. Mawasiliano na Kufunga Tukio</h2>
                    <p>Tuma hali ya urejeshaji kwa viongozi, rekodi hatua zilizochukuliwa, na fanya post-incident review inayopendekeza maboresho ya backup, hardening, na testing cadence.</p>
                </div>
                """;
        }
        return """
                <div class="section-card">
                    <h2>1. Runbook Purpose</h2>
                    <p>This runbook provides direct operational steps for administrators when FROMS must be restored quickly after a service outage or security incident.</p>
                </div>
                <div class="section-card">
                    <h2>2. Immediate Actions</h2>
                    <ul>
                        <li>Confirm the alert from monitoring, logs, or user escalation.</li>
                        <li>Determine whether the failure is application, database, storage, or network related.</li>
                        <li>Freeze new deployments until the initial assessment is complete.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>3. Database Recovery</h2>
                    <ol class="ui-manual-list">
                        <li>Select the latest verified backup.</li>
                        <li>Restore schema and data into the recovery environment.</li>
                        <li>Validate users, stations, incidents, emergency calls, and audit logs before admitting traffic.</li>
                    </ol>
                </div>
                <div class="section-card">
                    <h2>4. Service Recovery</h2>
                    <ul>
                        <li>Start the Spring Boot application with the approved environment configuration.</li>
                        <li>Verify database connectivity, storage directories, and the Python AI sidecar dependency.</li>
                        <li>Smoke-test `/login`, `/dashboard`, `/api/public/reports`, and `/api/admin/documents`.</li>
                    </ul>
                </div>
                <div class="section-card">
                    <h2>5. Communication and Closure</h2>
                    <p>Issue recovery status updates to leadership, record all actions taken, and complete a post-incident review that proposes backup, hardening, and test cadence improvements.</p>
                </div>
                """;
    }

    private String title(String key, String lang) {
        boolean sw = "sw".equalsIgnoreCase(lang);
        return switch (key) {
            case "system-architecture" -> sw ? "RIPOTI YA USANIFU WA MFUMO WA FROMS" : "FROMS SYSTEM ARCHITECTURE DOCUMENT";
            case "system-design-document" -> sw ? "RIPOTI YA MUUNDO WA MFUMO WA FROMS" : "FROMS SYSTEM DESIGN DOCUMENT";
            case "system-requirements-specification" -> sw ? "RIPOTI YA MAHITAJI YA MFUMO WA FROMS" : "FROMS SYSTEM REQUIREMENTS SPECIFICATION";
            case "disaster-recovery-plan" -> sw ? "MPANGO WA UOKOAJI WA MAJANGA WA FROMS" : "FROMS DISASTER RECOVERY PLAN";
            case "disaster-recovery-runbook" -> sw ? "MWONGOZO WA UREJESHAJI WA FROMS" : "FROMS DISASTER RECOVERY RUNBOOK";
            default -> sw ? "RIPOTI YA FROMS" : "FROMS REPORT";
        };
    }
}
