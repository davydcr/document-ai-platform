-- Add error_message column to documents table for storing OCR/processing failures
ALTER TABLE documents ADD COLUMN error_message TEXT;

-- Add comment for documentation
COMMENT ON COLUMN documents.error_message IS 'Stores error details when document processing fails (OCR error, classification error, etc.)';
