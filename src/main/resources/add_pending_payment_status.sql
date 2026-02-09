-- Migration to add PENDING_PAYMENT status to bookings table
-- Run this SQL command in your MySQL database

USE vehicle_booking;

-- Option 1: Modify the status column if it's an ENUM
-- (This will work if the column was created as ENUM)
ALTER TABLE bookings 
MODIFY COLUMN status ENUM('PENDING', 'PENDING_PAYMENT', 'ACCEPTED', 'COMPLETED', 'CANCELLED');

-- Option 2: If Option 1 fails, convert to VARCHAR (more flexible)
-- ALTER TABLE bookings 
-- MODIFY COLUMN status VARCHAR(20);

-- Verify the change
DESCRIBE bookings;
