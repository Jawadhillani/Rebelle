-- Rebelle Medical Practice Management Database Schema
-- SQLite Database

-- Enable foreign key constraints
PRAGMA foreign_keys = ON;

-- Patients table
CREATE TABLE IF NOT EXISTS patients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    address TEXT,
    date_of_birth TEXT,
    medical_notes TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Services/Procedures table
CREATE TABLE IF NOT EXISTS services (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    description TEXT,
    default_price DECIMAL(10,2) NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    is_active BOOLEAN DEFAULT 1
);

-- Appointments table
CREATE TABLE IF NOT EXISTS appointments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    patient_id INTEGER NOT NULL,
    service_id INTEGER,
    appointment_date TEXT NOT NULL,        -- ISO format: 2025-06-03
    appointment_time TEXT NOT NULL,        -- HH:MM format
    duration_minutes INTEGER DEFAULT 30,
    status TEXT DEFAULT 'scheduled',       -- scheduled, completed, cancelled, no_show
    notes TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);

-- Inventory table
CREATE TABLE IF NOT EXISTS inventory (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT,                         -- medicine, supplies, equipment
    quantity INTEGER NOT NULL DEFAULT 0,
    unit TEXT DEFAULT 'pieces',            -- pieces, bottles, boxes
    threshold INTEGER NOT NULL DEFAULT 5,  -- low stock warning
    cost_per_unit DECIMAL(10,2),
    supplier TEXT,
    expiry_date TEXT,
    notes TEXT,
    last_updated TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Inventory transactions (usage tracking)
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    inventory_id INTEGER NOT NULL,
    transaction_type TEXT NOT NULL,        -- add, remove, adjust
    quantity_change INTEGER NOT NULL,      -- positive for add, negative for remove
    reason TEXT,                          -- patient_use, expired, damaged, restock
    appointment_id INTEGER,               -- if used during appointment
    transaction_date TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (inventory_id) REFERENCES inventory(id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

-- Invoices table
CREATE TABLE IF NOT EXISTS invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_number TEXT UNIQUE NOT NULL,
    patient_id INTEGER NOT NULL,
    appointment_id INTEGER,
    service_id INTEGER,
    amount DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) DEFAULT 0,
    tax_amount DECIMAL(10,2) DEFAULT 0,
    total_amount DECIMAL(10,2) NOT NULL,
    status TEXT DEFAULT 'pending',         -- pending, partial, paid, overdue
    invoice_date TEXT NOT NULL,
    due_date TEXT,
    notes TEXT,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL,
    amount_paid DECIMAL(10,2) NOT NULL,
    payment_method TEXT NOT NULL,          -- cash, card, insurance, bank_transfer
    payment_date TEXT NOT NULL,
    reference_number TEXT,                 -- transaction ID, check number
    notes TEXT,
    created_at TEXT DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id)
);

-- AI Chat History (optional, for conversation context)
CREATE TABLE IF NOT EXISTS ai_conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_message TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    action_taken TEXT,                     -- JSON of executed commands
    timestamp TEXT DEFAULT CURRENT_TIMESTAMP
);

-- Application settings
CREATE TABLE IF NOT EXISTS settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    description TEXT
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_invoices_patient ON invoices(patient_id);
CREATE INDEX IF NOT EXISTS idx_payments_invoice ON payments(invoice_id);

-- Insert default services
INSERT OR IGNORE INTO services (name, description, default_price, duration_minutes) VALUES
('General Consultation', 'Regular check-up', 100.00, 30),
('Follow-up Visit', 'Follow-up consultation', 75.00, 20),
('Vaccination', 'Vaccine administration', 50.00, 15),
('Blood Pressure Check', 'BP monitoring', 25.00, 10);

-- Insert default settings
INSERT OR IGNORE INTO settings (key, value, description) VALUES
('clinic_name', 'Rebelle Medical Practice', 'Practice name'),
('working_hours_start', '09:00', 'Daily start time'),
('working_hours_end', '17:00', 'Daily end time'),
('appointment_duration_default', '30', 'Default appointment length in minutes'),
('currency', 'USD', 'Currency for billing'); 