# MVP consistency limitation (ADR-005)

Account Service owns balances (`POST /api/accounts/{id}/deposit`).
Transaction Service owns history (`POST /api/transactions`) and only verifies account
existence via REST. These are separate operations; the MVP does not provide atomic
consistency between balance and history if one step fails.
