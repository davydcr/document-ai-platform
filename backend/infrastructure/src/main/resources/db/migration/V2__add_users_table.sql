-- V2: Create users and roles tables
-- Add user authentication support with role-based access control

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_role_name ON roles(name);

CREATE TABLE user_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_email ON user_accounts(email);
CREATE INDEX idx_user_active ON user_accounts(active);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES user_accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrator with full access'),
('USER', 'Standard user access'),
('ANALYST', 'Data analyst access'),
('VIEWER', 'Read-only access');

-- Insert seed admin user (password: admin123 -> will be hashed with BCrypt)
-- This is a placeholder - actual hash will be inserted in the seed script
INSERT INTO user_accounts (email, first_name, last_name, password_hash, active) VALUES
('admin@example.com', 'Admin', 'User', '$2a$10$slYQmyNdGzin7olVN3p5be4DlH.PKZbv5H8KnzzVgXXbVxzy990qm', true);

-- Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id) 
SELECT ua.id, r.id FROM user_accounts ua, roles r 
WHERE ua.email = 'admin@example.com' AND r.name = 'ADMIN';
