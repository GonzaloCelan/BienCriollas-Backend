CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(150) NOT NULL,
    hourly_rate DECIMAL(19,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    notes VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_employees_name CHECK (CHAR_LENGTH(TRIM(name)) > 0),
    CONSTRAINT ck_employees_hourly_rate CHECK (hourly_rate > 0),
    INDEX idx_employees_active_name (active, name),
    INDEX idx_employees_name (name)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE employee_work_days (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    hourly_rate_snapshot DECIMAL(19,2) NOT NULL,
    total_worked_minutes INT NOT NULL,
    total_amount DECIMAL(19,2) NOT NULL,
    notes VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_employee_work_day_employee FOREIGN KEY (employee_id)
        REFERENCES employees(id),
    CONSTRAINT uk_employee_work_day_employee_date UNIQUE (employee_id, work_date),
    CONSTRAINT ck_employee_work_day_hourly_rate CHECK (hourly_rate_snapshot > 0),
    CONSTRAINT ck_employee_work_day_minutes CHECK (total_worked_minutes > 0),
    CONSTRAINT ck_employee_work_day_amount CHECK (total_amount >= 0),
    INDEX idx_employee_work_days_date (work_date),
    INDEX idx_employee_work_days_employee_date (employee_id, work_date)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE employee_work_shifts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    work_day_id BIGINT NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_minutes INT NOT NULL DEFAULT 0,
    worked_minutes INT NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_employee_work_shift_work_day FOREIGN KEY (work_day_id)
        REFERENCES employee_work_days(id) ON DELETE CASCADE,
    CONSTRAINT ck_employee_work_shift_time CHECK (end_time > start_time),
    CONSTRAINT ck_employee_work_shift_break CHECK (break_minutes >= 0),
    CONSTRAINT ck_employee_work_shift_minutes CHECK (worked_minutes > 0),
    CONSTRAINT ck_employee_work_shift_sort_order CHECK (sort_order > 0),
    INDEX idx_employee_work_shifts_work_day (work_day_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
