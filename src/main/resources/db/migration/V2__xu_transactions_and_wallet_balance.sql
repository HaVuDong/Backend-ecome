-- Add xu_transactions table and wallet_balance column to users
ALTER TABLE users ADD COLUMN wallet_balance DOUBLE DEFAULT 0;

CREATE TABLE xu_transactions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  amount BIGINT NOT NULL,
  vnp_txn_ref VARCHAR(255) UNIQUE,
  type VARCHAR(50),
  status VARCHAR(50),
  description TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_xu_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_xu_vnp_txn_ref ON xu_transactions(vnp_txn_ref);
CREATE INDEX idx_xu_user_created_at ON xu_transactions(user_id, created_at DESC);
