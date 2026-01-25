package com.davydcr.document.domain.model;

/**
 * Estados de um documento no sistema.
 * 
 * Estados válidos:
 * - RECEIVED: Documento recebido, pronto para processamento
 * - PROCESSING: Documento em processamento (OCR, classificação)
 * - COMPLETED: Processamento concluído com sucesso
 * - FAILED: Processamento falhou
 */
public enum DocumentStatus {
    RECEIVED("Recebido"),
    PROCESSING("Processando"),
    COMPLETED("Completado"),
    FAILED("Falhou");

    private final String displayName;

    DocumentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Valida se é possível transicionar para o estado seguinte.
     */
    public boolean canTransitionTo(DocumentStatus nextStatus) {
        if (nextStatus == null) {
            return false;
        }

        return switch (this) {
            case RECEIVED -> nextStatus == PROCESSING;
            case PROCESSING -> nextStatus == COMPLETED || nextStatus == FAILED;
            case COMPLETED, FAILED -> false; // Estados finais, sem transições
        };
    }
}
