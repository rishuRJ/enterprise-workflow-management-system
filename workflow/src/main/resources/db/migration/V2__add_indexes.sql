-- Indexes for the queries the application runs most often.
-- PostgreSQL does not index foreign key columns on its own, so without these
-- every lookup below reads the whole table.

-- GET /requests/my: requests of one employee, newest first, paginated
create index idx_requests_employee_created_at
    on requests (employee_id, created_at desc);

-- GET /requests/manager: requests assigned to one manager, newest first, paginated
create index idx_requests_manager_created_at
    on requests (manager_id, created_at desc);

-- Request history of one request, oldest first
create index idx_request_history_request_occurred_at
    on request_history (request_id, occurred_at);

-- Outbox poller and stale-event recovery: both scan unfinished events in id order.
-- Partial index: it only holds events that still need work, so it stays small
-- while PROCESSED and PERMANENTLY_FAILED rows keep growing.
create index idx_outbox_event_unfinished
    on outbox_event (id)
    where status in ('PENDING', 'FAILED', 'PROCESSING');
