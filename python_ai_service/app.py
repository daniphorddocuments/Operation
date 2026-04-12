import json
import os
from http.server import BaseHTTPRequestHandler, HTTPServer


HOST = os.getenv("FROMS_AI_HOST", "127.0.0.1")
PORT = int(os.getenv("FROMS_AI_PORT", "8091"))


SEVERITY_WEIGHTS = {
    "LOW": 20,
    "MEDIUM": 45,
    "HIGH": 80,
    "CRITICAL": 95,
}


INCIDENT_TYPE_WEIGHTS = {
    "FIRE": 30,
    "RESCUE": 24,
    "ACCIDENT": 18,
}


def normalize(value):
    return (value or "").strip().upper()


def build_recommendation(payload):
    incident_type = normalize(payload.get("incidentType"))
    severity = normalize(payload.get("severity"))
    status = normalize(payload.get("status"))
    details = (payload.get("details") or "").lower()
    resources_used = (payload.get("resourcesUsed") or "").lower()
    ward = payload.get("ward") or ""
    village = payload.get("village") or ""
    road_landmark = payload.get("roadLandmark") or ""
    distance_km = float(payload.get("distanceKm") or 0.0)
    eta_minutes = float(payload.get("etaMinutes") or 0.0)
    district_incident_count = int(payload.get("districtIncidentCount") or 0)
    district_average_response = float(payload.get("districtAverageResponseTime") or 0.0)

    severity_score = SEVERITY_WEIGHTS.get(severity, 35)
    type_score = INCIDENT_TYPE_WEIGHTS.get(incident_type, 10)
    detail_score = 0

    if "smoke" in details or "trapped" in details or "explosion" in details:
        detail_score += 12
    if "school" in details or "hospital" in details or "market" in details:
        detail_score += 10
    if "chemical" in details or "fuel" in details or "gas" in details:
        detail_score += 14
    if "rescue" in resources_used or "ambulance" in resources_used:
        detail_score += 6
    if status == "ACTIVE":
        detail_score += 5

    risk_score = min(100, severity_score + type_score + detail_score)

    if risk_score >= 85:
        priority = "CRITICAL"
    elif risk_score >= 65:
        priority = "HIGH"
    elif risk_score >= 40:
        priority = "MEDIUM"
    else:
        priority = "LOW"

    actions = []

    if incident_type == "FIRE":
        actions.append("Dispatch nearest suppression unit immediately")
        actions.append("Prepare evacuation and perimeter control")
    elif incident_type == "RESCUE":
        actions.append("Dispatch rescue-trained officers")
        actions.append("Coordinate medical support if casualties are possible")
    elif incident_type == "ACCIDENT":
        actions.append("Secure the scene and assess casualty risk")
        actions.append("Coordinate traffic and rescue access routes")
    else:
        actions.append("Assign nearest available operational unit")

    if severity in {"HIGH", "CRITICAL"}:
        actions.append("Raise command visibility for this incident")
    if "chemical" in details or "fuel" in details or "gas" in details:
        actions.append("Treat scene as hazardous-material risk until confirmed safe")
    if "market" in details or "school" in details or "hospital" in details:
        actions.append("Prioritize crowd protection and access control")
    if distance_km > 15:
        actions.append("Pre-alert regional support because the travel distance is extended")
    if ward or village or road_landmark:
        actions.append(f"Use the mapped route for {road_landmark or village or ward} and keep location confirmation open with the caller")

    improvements = []
    if district_average_response > 20:
        improvements.append("Reduce district response time by repositioning standby teams closer to recurring hotspots")
    if district_incident_count >= 5:
        improvements.append("Run district prevention outreach and pre-incident inspections in repeatedly affected wards")
    if distance_km > 12:
        improvements.append("Improve road access intelligence and verify alternate station-to-scene routes for distant incidents")
    if eta_minutes > 18:
        improvements.append("Strengthen dispatch readiness checks so turnout time drops before vehicles leave station")
    if "fuel" in details or "gas" in details or "chemical" in details:
        improvements.append("Increase hazmat readiness, foam stock review, and multi-agency drill frequency for high-risk materials")
    if not improvements:
        improvements.append("Continue capturing accurate ward, street, and landmark data so routing and deployment analytics keep improving")

    return {
        "severityScore": severity_score,
        "riskScore": risk_score,
        "priority": priority,
        "recommendedActions": actions,
        "operationalImprovements": improvements,
        "model": "python-heuristic-v1",
    }


def build_system_audit(payload):
    snapshot = payload.get("snapshot") if isinstance(payload.get("snapshot"), dict) else payload
    checks = payload.get("checks") or []
    runtime_status = payload.get("runtimeStatus") or {}

    weak_passwords = int(snapshot.get("weakPasswords") or 0)
    inactive_users = int(snapshot.get("inactiveUsers") or 0)
    failed_logins = int(snapshot.get("failedLoginsSinceLastTest") or 0)
    average_query_latency = float(snapshot.get("averageQueryLatencyMs") or 0.0)
    average_response_time = float(snapshot.get("averageResponseTimeMinutes") or 0.0)
    vulnerabilities = snapshot.get("vulnerabilities") or []

    risk_score = 10
    findings = []
    recommended_actions = []
    pass_count = 0
    warn_count = 0
    fail_count = 0

    for check in checks:
        status = normalize(check.get("status"))
        if status == "PASS":
            pass_count += 1
        elif status == "WARN":
            warn_count += 1
        elif status == "FAIL":
            fail_count += 1

    if fail_count > 0:
        risk_score += min(30, fail_count * 6)
        findings.append(f"{fail_count} system verification check(s) failed")
        recommended_actions.append("Resolve failed verification checks before the next production review")
    if warn_count > 0:
        risk_score += min(18, warn_count * 2)
        findings.append(f"{warn_count} verification warning(s) require follow-up")

    if weak_passwords > 0:
        risk_score += min(35, weak_passwords * 5)
        findings.append(f"Weak passwords detected for {weak_passwords} account(s)")
        recommended_actions.append("Force password reset for weak accounts before the next review")
    if inactive_users > 0:
        risk_score += min(15, inactive_users * 2)
        findings.append(f"{inactive_users} inactive account(s) should be reviewed")
        recommended_actions.append("Disable or revalidate dormant accounts")
    if failed_logins > 0:
        risk_score += min(20, failed_logins * 2)
        findings.append(f"{failed_logins} failed login event(s) occurred since the last test")
        recommended_actions.append("Review audit logs for repeated access failures and lockout activity")
    if average_query_latency > 350:
        risk_score += 12
        findings.append(f"Average repository latency is elevated at {average_query_latency:.1f} ms")
        recommended_actions.append("Profile slow queries and database access paths")
    if average_response_time > 20:
        risk_score += 10
        findings.append(f"Operational response time is high at {average_response_time:.1f} minutes")
        recommended_actions.append("Review dispatch workflow and station readiness")
    if vulnerabilities:
        risk_score += min(20, len(vulnerabilities) * 4)
        findings.extend(vulnerabilities[:4])

    login_page = runtime_status.get("loginPage") or {}
    public_report_page = runtime_status.get("publicReportPage") or {}
    ai_sidecar = runtime_status.get("aiSidecar") or {}

    if normalize(login_page.get("status")) != "UP":
        risk_score += 20
        findings.append("Application login route is not healthy")
        recommended_actions.append("Restore local application runtime before trusting system-test output")
    if normalize(public_report_page.get("status")) != "UP":
        risk_score += 12
        findings.append("Public emergency reporting route is not healthy")
        recommended_actions.append("Verify public emergency access and routing immediately")
    if normalize(ai_sidecar.get("status")) != "OK":
        risk_score += 10
        findings.append("Python AI sidecar is unavailable")
        recommended_actions.append("Restart the Python AI sidecar so AI-backed testing remains active")

    if risk_score >= 70:
        risk_level = "HIGH"
    elif risk_score >= 40:
        risk_level = "MEDIUM"
    else:
        risk_level = "LOW"

    if not findings:
        findings.append("No major internal risk indicators were raised by the current audit payload")
        recommended_actions.append("Keep the six-month test cycle active")

    executive_summary = (
        f"AI reviewed {pass_count + warn_count + fail_count} verification checks and rated the current system posture "
        f"as {risk_level}. Key pressure points are weak credentials, runtime health, failed logins, latency, and "
        f"any unresolved verification failures."
    )

    return {
        "riskLevel": risk_level,
        "riskScore": min(100, risk_score),
        "executiveSummary": executive_summary,
        "findings": findings,
        "recommendedActions": recommended_actions,
        "model": "python-system-audit-v2",
    }


class Handler(BaseHTTPRequestHandler):
    def _send_json(self, status_code, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path == "/health":
            self._send_json(200, {"status": "ok", "service": "python-ai-sidecar"})
            return
        self._send_json(404, {"error": "Not found"})

    def do_POST(self):
        if self.path not in {"/recommend", "/system-audit"}:
            self._send_json(404, {"error": "Not found"})
            return

        content_length = int(self.headers.get("Content-Length", "0"))
        raw = self.rfile.read(content_length)
        try:
            payload = json.loads(raw or b"{}")
        except json.JSONDecodeError:
            self._send_json(400, {"error": "Invalid JSON"})
            return

        if self.path == "/recommend":
            self._send_json(200, build_recommendation(payload))
            return

        self._send_json(200, build_system_audit(payload))

    def log_message(self, format, *args):
        return


if __name__ == "__main__":
    server = HTTPServer((HOST, PORT), Handler)
    print(f"Python AI sidecar running on http://{HOST}:{PORT}")
    server.serve_forever()
