Place official Tanzania Mainland geography CSV files here for startup import.

Expected file:
- `tz_mainland_localities.csv`

Required columns:
- `region`
- `district`
- `ward`
- `entry_type`
- `locality_name`

Optional columns:
- `region_code`
- `ward_latitude`
- `ward_longitude`
- `locality_latitude`
- `locality_longitude`
- `landmark_name`
- `landmark_symbol`
- `landmark_latitude`
- `landmark_longitude`

Notes:
- `entry_type` accepts `VILLAGE`, `STREET`, `KIJIJI`, or `MTAA`
- Use official source data only
- Regions and districts must match Tanzania Mainland official names used by the system
- If this file is missing, the system falls back to generated placeholder ward/locality data
