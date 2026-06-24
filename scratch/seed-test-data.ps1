# seed-test-data.ps1
# Seeding script for CryptoVault local development database to verify cross-wallet transactions and KYC.

$Sql = @"
-- Ensure users exist
INSERT INTO users (id, created_at, email, name, password, role, updated_at)
VALUES (
  '90942a2f-cb35-48f1-a52d-33a6a345135f',
  NOW(),
  'atharv.khisti2004@gmail.com',
  'atharv khisti',
  '$2a$10$sOri5cCdP0Gz7M/NUSXi3.TQlFQ3pRwDIJ78nKsQjQlov8nxPOdMu',
  'USER',
  NOW()
) ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, created_at, email, name, password, role, updated_at)
VALUES (
  '565507e2-8471-47ce-be7f-37cae10e514c',
  NOW(),
  'niftyfindshub@gmail.com',
  'dyson sphere',
  '$2a$10$sMbSABPoic3owhHs4vAOD.LMdy.UXT0WJi5b/M8NJFXRr2uzn.Cpi',
  'USER',
  NOW()
) ON CONFLICT (id) DO NOTHING;

-- Ensure wallets exist
INSERT INTO wallets (id, balance, created_at, currency, updated_at, user_id)
VALUES (
  '078a37f1-eca6-4179-9584-d506c1a5b8c7',
  10.0,
  NOW(),
  'BTC',
  NOW(),
  '90942a2f-cb35-48f1-a52d-33a6a345135f'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO wallets (id, balance, created_at, currency, updated_at, user_id)
VALUES (
  'a938fc10-ff8e-45a2-80a8-0283a1a15656',
  1.0,
  NOW(),
  'BTC',
  NOW(),
  '565507e2-8471-47ce-be7f-37cae10e514c'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO wallets (id, balance, created_at, currency, updated_at, user_id)
VALUES (
  '81c9ab02-9a52-4685-8bca-2e80f8513d32',
  10.5,
  NOW(),
  'ETH',
  NOW(),
  '565507e2-8471-47ce-be7f-37cae10e514c'
) ON CONFLICT (id) DO NOTHING;

-- Ensure audit logs exist for active address book
INSERT INTO audit_logs (id, action, created_at, description, event_timestamp, event_type, ip_address, performed_by, service_name, user_id)
VALUES (
  'bde7c5cf-69a1-406e-8f8e-40427693f8a1',
  'WALLET_CREATION',
  NOW(),
  'Wallet ID: 078a37f1-eca6-4179-9584-d506c1a5b8c7, User: atharv.khisti2004@gmail.com, Currency: BTC',
  NOW(),
  'WALLET_CREATED',
  '127.0.0.1',
  'SYSTEM',
  'wallet-service',
  '90942a2f-cb35-48f1-a52d-33a6a345135f'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO audit_logs (id, action, created_at, description, event_timestamp, event_type, ip_address, performed_by, service_name, user_id)
VALUES (
  'e87b7a2d-2098-466d-8a0a-e24c6533ea40',
  'WALLET_CREATION',
  NOW(),
  'Wallet ID: a938fc10-ff8e-45a2-80a8-0283a1a15656, User: niftyfindshub@gmail.com, Currency: BTC',
  NOW(),
  'WALLET_CREATED',
  '127.0.0.1',
  'SYSTEM',
  'wallet-service',
  '565507e2-8471-47ce-be7f-37cae10e514c'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO audit_logs (id, action, created_at, description, event_timestamp, event_type, ip_address, performed_by, service_name, user_id)
VALUES (
  'f8e97a2d-2098-466d-8a0a-e24c6533ea41',
  'WALLET_CREATION',
  NOW(),
  'Wallet ID: 81c9ab02-9a52-4685-8bca-2e80f8513d32, User: niftyfindshub@gmail.com, Currency: ETH',
  NOW(),
  'WALLET_CREATED',
  '127.0.0.1',
  'SYSTEM',
  'wallet-service',
  '565507e2-8471-47ce-be7f-37cae10e514c'
) ON CONFLICT (id) DO NOTHING;
"@

Write-Host "Connecting to Docker Postgres DB 'cryptovault' to seed test data..." -ForegroundColor Yellow
$result = docker exec -i postgres-db psql -U postgres -d cryptovault -c $Sql

if ($LASTEXITCODE -eq 0) {
    Write-Host "Database successfully seeded!" -ForegroundColor Green
    Write-Host $result
} else {
    Write-Error "Database seeding failed. Ensure Docker and postgres-db container are running."
}
