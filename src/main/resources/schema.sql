
-- =====================================================
-- 1. EMPLOYEES TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS employees (
                                         id TEXT PRIMARY KEY,
                                         name TEXT NOT NULL,
                                         username TEXT UNIQUE NOT NULL,
                                         password TEXT NOT NULL,
                                         mobile TEXT,
                                         designation TEXT NOT NULL DEFAULT 'Forest Range Officer',
                                         division TEXT NOT NULL DEFAULT 'Guntur Division',
                                         district TEXT NOT NULL DEFAULT 'Guntur',
                                         role TEXT NOT NULL DEFAULT 'employee',
                                         date_of_joining DATE,
                                         created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

-- Employees migrations

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS name TEXT;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS username TEXT;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS password TEXT;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS mobile TEXT;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS designation TEXT NOT NULL DEFAULT 'Forest Range Officer';

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS division TEXT NOT NULL DEFAULT 'Guntur Division';

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS district TEXT NOT NULL DEFAULT 'Guntur';

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS role TEXT NOT NULL DEFAULT 'employee';

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS date_of_joining DATE;

ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();


-- =====================================================
-- 2. ATTENDANCE TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS attendance (
                                          id BIGSERIAL PRIMARY KEY,
                                          employee_id TEXT NOT NULL,
                                          clock_in TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    clock_out TIMESTAMPTZ,
    created_at DATE NOT NULL DEFAULT CURRENT_DATE,
    clock_in_latitude DOUBLE PRECISION,
    clock_in_longitude DOUBLE PRECISION,
    clock_out_latitude DOUBLE PRECISION,
    clock_out_longitude DOUBLE PRECISION
    );

-- Attendance migrations

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS employee_id TEXT;

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS clock_in TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS clock_out TIMESTAMPTZ;

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS created_at DATE NOT NULL DEFAULT CURRENT_DATE;

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS clock_in_latitude DOUBLE PRECISION;

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS clock_in_longitude DOUBLE PRECISION;

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS clock_out_latitude DOUBLE PRECISION;

ALTER TABLE attendance
    ADD COLUMN IF NOT EXISTS clock_out_longitude DOUBLE PRECISION;


-- =====================================================
-- 3. LEAVE REQUESTS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS leave_requests (
                                              id BIGSERIAL PRIMARY KEY,
                                              employee_id TEXT NOT NULL,
                                              leave_type TEXT NOT NULL,
                                              from_date DATE NOT NULL,
                                              to_date DATE NOT NULL,
                                              reason TEXT NOT NULL,
                                              status TEXT NOT NULL DEFAULT 'Pending',
                                              created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    reviewed_by TEXT,
    CHECK (to_date >= from_date)
    );

-- Leave requests migrations

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS employee_id TEXT;

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS leave_type TEXT;

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS from_date DATE;

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS to_date DATE;

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS reason TEXT;

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS status TEXT NOT NULL DEFAULT 'Pending';

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ;

ALTER TABLE leave_requests
    ADD COLUMN IF NOT EXISTS reviewed_by TEXT;


-- =====================================================
-- 4. FACE TEMPLATES TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS face_templates (
                                              employee_id TEXT PRIMARY KEY,
                                              descriptor DOUBLE PRECISION[] NOT NULL,
                                              registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (cardinality(descriptor) = 128)
    );

-- Face templates migrations

ALTER TABLE face_templates
    ADD COLUMN IF NOT EXISTS employee_id TEXT;

ALTER TABLE face_templates
    ADD COLUMN IF NOT EXISTS descriptor DOUBLE PRECISION[];

ALTER TABLE face_templates
    ADD COLUMN IF NOT EXISTS registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW();


-- =====================================================
-- 5. AUTH SESSIONS TABLE
-- =====================================================

CREATE TABLE IF NOT EXISTS auth_sessions (
                                             token TEXT PRIMARY KEY,
                                             employee_id TEXT NOT NULL,
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

-- Auth sessions migrations

ALTER TABLE auth_sessions
    ADD COLUMN IF NOT EXISTS employee_id TEXT;

ALTER TABLE auth_sessions
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();


-- =====================================================
-- 6. FOREIGN KEY CONSTRAINTS
-- =====================================================

-- Attendance

ALTER TABLE attendance
DROP CONSTRAINT IF EXISTS attendance_employee_id_fkey;

ALTER TABLE attendance
    ADD CONSTRAINT attendance_employee_id_fkey
        FOREIGN KEY (employee_id)
            REFERENCES employees(id)
            ON DELETE CASCADE
            ON UPDATE CASCADE;


-- Leave requests employee

ALTER TABLE leave_requests
DROP CONSTRAINT IF EXISTS leave_requests_employee_id_fkey;

ALTER TABLE leave_requests
    ADD CONSTRAINT leave_requests_employee_id_fkey
        FOREIGN KEY (employee_id)
            REFERENCES employees(id)
            ON DELETE CASCADE
            ON UPDATE CASCADE;


-- Leave requests reviewer

ALTER TABLE leave_requests
DROP CONSTRAINT IF EXISTS leave_requests_reviewed_by_fkey;

ALTER TABLE leave_requests
    ADD CONSTRAINT leave_requests_reviewed_by_fkey
        FOREIGN KEY (reviewed_by)
            REFERENCES employees(id)
            ON DELETE SET NULL
            ON UPDATE CASCADE;


-- Face templates

ALTER TABLE face_templates
DROP CONSTRAINT IF EXISTS face_templates_employee_id_fkey;

ALTER TABLE face_templates
    ADD CONSTRAINT face_templates_employee_id_fkey
        FOREIGN KEY (employee_id)
            REFERENCES employees(id)
            ON DELETE CASCADE
            ON UPDATE CASCADE;


-- Auth sessions

ALTER TABLE auth_sessions
DROP CONSTRAINT IF EXISTS auth_sessions_employee_id_fkey;

ALTER TABLE auth_sessions
    ADD CONSTRAINT auth_sessions_employee_id_fkey
        FOREIGN KEY (employee_id)
            REFERENCES employees(id)
            ON DELETE CASCADE
            ON UPDATE CASCADE;


-- =====================================================
-- 7. INDEXES
-- =====================================================

CREATE INDEX IF NOT EXISTS attendance_employee_date_idx
    ON attendance(employee_id, created_at DESC);

CREATE INDEX IF NOT EXISTS leave_requests_employee_idx
    ON leave_requests(employee_id);

CREATE INDEX IF NOT EXISTS leave_requests_status_idx
    ON leave_requests(status);

CREATE INDEX IF NOT EXISTS auth_sessions_employee_idx
    ON auth_sessions(employee_id);


-- =====================================================
-- 8. DEFAULT EMPLOYEE
-- =====================================================

INSERT INTO employees (
    id,
    name,
    username,
    password,
    mobile,
    role
)
SELECT
    'EMP001',
    'Ashok Kumar',
    'employee',
    '123456',
    '9876543210',
    'employee'
    WHERE NOT EXISTS (
    SELECT 1 FROM employees
)
ON CONFLICT (id) DO NOTHING;


-- =====================================================
-- 9. DEFAULT ADMIN
-- =====================================================

