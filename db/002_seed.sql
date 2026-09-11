INSERT INTO files (id, name, description, document_type) VALUES
('10000000-0000-0000-0000-000000000001', 'API Specification', 'External and internal API contracts exposed by the platform.', 'specification'),
('10000000-0000-0000-0000-000000000002', 'Authentication Design', 'Authentication, authorization, tokens, and session behavior.', 'design'),
('10000000-0000-0000-0000-000000000003', 'Payment Service Design', 'Payment service interfaces and processing rules.', 'design'),
('10000000-0000-0000-0000-000000000004', 'Checkout Service Design', 'Checkout orchestration and order creation flow.', 'design'),
('10000000-0000-0000-0000-000000000005', 'Mobile Client Specification', 'Mobile application contract and client-side API expectations.', 'specification'),
('10000000-0000-0000-0000-000000000006', 'Database Schema', 'Relational schema used by core services.', 'schema'),
('10000000-0000-0000-0000-000000000007', 'QA Test Plan', 'Integration and regression test coverage.', 'test_plan'),
('10000000-0000-0000-0000-000000000008', 'Deployment Runbook', 'Operational deployment and rollback procedures.', 'runbook');

INSERT INTO file_versions (id, file_id, version_number, content, content_hash, is_current) VALUES
('20000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000001',1,'API v1: POST /payments accepts amount, currency, and customer_id.','sha-api-v1',FALSE),
('20000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000001',2,'API v2: POST /payments additionally requires idempotency_key.','sha-api-v2',FALSE),
('20000000-0000-0000-0000-000000000003','10000000-0000-0000-0000-000000000001',3,'API v3: payment creation requires idempotency_key and returns payment_intent_id.','sha-api-v3',TRUE),
('20000000-0000-0000-0000-000000000004','10000000-0000-0000-0000-000000000002',1,'Authentication uses OAuth-style bearer tokens and role-based authorization.','sha-auth-v1',TRUE),
('20000000-0000-0000-0000-000000000005','10000000-0000-0000-0000-000000000003',1,'Payment service consumes the payments API, stores payment intents, and persists transaction state.','sha-payment-v1',TRUE),
('20000000-0000-0000-0000-000000000006','10000000-0000-0000-0000-000000000004',1,'Checkout service creates orders and calls the payment service during checkout.','sha-checkout-v1',TRUE),
('20000000-0000-0000-0000-000000000007','10000000-0000-0000-0000-000000000005',1,'Mobile client calls checkout and payment endpoints and expects documented response fields.','sha-mobile-v1',TRUE),
('20000000-0000-0000-0000-000000000008','10000000-0000-0000-0000-000000000006',1,'Database contains orders, payment_intents, and transactions tables.','sha-db-v1',TRUE),
('20000000-0000-0000-0000-000000000009','10000000-0000-0000-0000-000000000007',1,'QA covers API contract tests, checkout flows, and payment regressions.','sha-qa-v1',TRUE),
('20000000-0000-0000-0000-000000000010','10000000-0000-0000-0000-000000000008',1,'Deployment runbook covers API, checkout, payment, and database migration rollout.','sha-runbook-v1',TRUE);

INSERT INTO file_relationships (id, source_file_id, target_file_id, relationship_type, confidence, evidence) VALUES
('30000000-0000-0000-0000-000000000001','10000000-0000-0000-0000-000000000003','10000000-0000-0000-0000-000000000001','depends_on',0.98,'Payment service implements the documented payment API.'),
('30000000-0000-0000-0000-000000000002','10000000-0000-0000-0000-000000000004','10000000-0000-0000-0000-000000000003','depends_on',0.98,'Checkout service invokes payment service behavior.'),
('30000000-0000-0000-0000-000000000003','10000000-0000-0000-0000-000000000005','10000000-0000-0000-0000-000000000004','depends_on',0.95,'Mobile checkout flow follows checkout service contract.'),
('30000000-0000-0000-0000-000000000004','10000000-0000-0000-0000-000000000003','10000000-0000-0000-0000-000000000006','depends_on',0.93,'Payment service persists payment state in the database schema.'),
('30000000-0000-0000-0000-000000000005','10000000-0000-0000-0000-000000000007','10000000-0000-0000-0000-000000000005','depends_on',0.90,'QA test cases assert mobile-facing API behavior.'),
('30000000-0000-0000-0000-000000000006','10000000-0000-0000-0000-000000000007','10000000-0000-0000-0000-000000000004','depends_on',0.96,'QA validates checkout orchestration.'),
('30000000-0000-0000-0000-000000000007','10000000-0000-0000-0000-000000000008','10000000-0000-0000-0000-000000000006','references',0.88,'Runbook documents database migration rollout.'),
('30000000-0000-0000-0000-000000000008','10000000-0000-0000-0000-000000000008','10000000-0000-0000-0000-000000000003','references',0.82,'Runbook contains payment deployment steps.');
