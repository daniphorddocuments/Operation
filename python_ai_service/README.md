# Python AI Sidecar

This folder adds an optional Python sidecar service for decision support.

It is intentionally separate from the core Java/Spring system.

## Why this exists

Python is a better fit for:
- AI/ML scoring
- analytics experiments
- video or image analysis
- NLP and speech workflows
- rapid prototyping of recommendation engines

The Java system should remain the source of truth for:
- authentication
- RBAC
- workflows
- dashboards
- audit and transactional logic

## What this service does

Current endpoints:
- `GET /health`
- `POST /recommend`

`/recommend` accepts incident data and returns:
- severity score
- risk score
- response priority
- recommended actions

## Run

Use Python 3.10+:

```powershell
python python_ai_service\app.py
```

Or use the helper script:

```powershell
scripts\run-ai-service.cmd
```

Default port:

`8091`

## Example request

```json
{
  "incidentType": "FIRE",
  "severity": "HIGH",
  "source": "PUBLIC_PORTAL",
  "status": "ACTIVE",
  "details": "Warehouse fire with smoke spreading to nearby shops",
  "resourcesUsed": "Water tender, rescue unit"
}
```

## Example response

```json
{
  "severityScore": 80,
  "riskScore": 89,
  "priority": "CRITICAL",
  "recommendedActions": [
    "Dispatch nearest suppression unit immediately",
    "Raise command visibility for this incident",
    "Prepare evacuation and perimeter control"
  ],
  "model": "python-heuristic-v1"
}
```

## Next step

If you want, I can wire the Java app to call this sidecar from the recommendation flow.
