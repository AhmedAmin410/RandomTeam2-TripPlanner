# RandomTeam2 Trip Planner

## Running M3 Normally

M3 uses isolated PostgreSQL databases per service. This is the default mode.

```powershell
docker compose up -d --build
powershell -ExecutionPolicy Bypass -File scripts\verify-m3.ps1
```

The M3 verifier checks the split datasource configuration plus the M3 HTTP feature and saga flows.

## Running the M2 Grader

The M2 grader expects all relational tables in one shared PostgreSQL database. Do not run the M2 grader against the default M3 compose mode.

Start the M2 compatibility stack first:

```powershell
docker compose -f docker-compose.yaml -f docker-compose.m2-grader.yaml up -d --build
```

Then run the M2 grader or M2 public test pack against the running services.

The compatibility override points all app services at:

```text
jdbc:postgresql://user-postgres:5432/tripdb-bookings
```

## Switching Back To M3

After running the M2 grader, restore normal M3 mode:

```powershell
docker compose up -d --build
```

## Verified State

Current verified results:

- M2 public grader pack: 425/425 tests passed
- M3 verifier: 105/105 checks passed
