package com.davydcr.document.infrastructure.service;

import org.springframework.stereotype.Service;
import java.util.Map;

/**
 * Serviço para renderizar templates de email.
 * 
 * Fornece templates HTML para diferentes tipos de alertas de segurança.
 */
@Service
public class EmailTemplateService {

    /**
     * Renderiza um template de email com as variáveis fornecidas
     */
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        return switch (templateName) {
            case "brute-force-alert" -> renderBruteForceAlert(variables);
            case "suspicious-activity-alert" -> renderSuspiciousActivityAlert(variables);
            case "anomalous-ip-alert" -> renderAnomalousIpAlert(variables);
            case "daily-security-report" -> renderDailySecurityReport(variables);
            case "new-location-notification" -> renderNewLocationNotification(variables);
            default -> renderDefaultTemplate(templateName, variables);
        };
    }

    /**
     * Template para alerta de brute force
     */
    private String renderBruteForceAlert(Map<String, Object> vars) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #d32f2f; color: white; padding: 20px; border-radius: 5px; }
                    .content { padding: 20px; background-color: #f5f5f5; border-radius: 5px; margin: 20px 0; }
                    .alert-box { background-color: #ffebee; border-left: 4px solid #d32f2f; padding: 15px; margin: 10px 0; }
                    .footer { color: #999; font-size: 12px; text-align: center; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🚨 Alerta de Brute Force</h2>
                    </div>
                    <div class="content">
                        <p>Um padrão de brute force foi detectado na sua plataforma.</p>
                        
                        <div class="alert-box">
                            <p><strong>Email do usuário:</strong> %s</p>
                            <p><strong>Endereço IP:</strong> %s</p>
                            <p><strong>Número de tentativas:</strong> %d</p>
                            <p><strong>Data/Hora:</strong> %s</p>
                        </div>
                        
                        <p><strong>Ações recomendadas:</strong></p>
                        <ul>
                            <li>Monitorar este IP para atividades adicionais</li>
                            <li>Considerar bloquear o IP temporariamente</li>
                            <li>Contatar o usuário para verificar a conta</li>
                            <li>Revisar o relatório de auditoria completo no painel de admin</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>Este é um email automático de segurança. Não responda a este email.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            vars.getOrDefault("userEmail", "N/A"),
            vars.getOrDefault("ipAddress", "N/A"),
            vars.getOrDefault("attemptCount", 0),
            vars.getOrDefault("timestamp", "N/A")
        );
    }

    /**
     * Template para alerta de atividade suspeita
     */
    private String renderSuspiciousActivityAlert(Map<String, Object> vars) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #ff9800; color: white; padding: 20px; border-radius: 5px; }
                    .content { padding: 20px; background-color: #f5f5f5; border-radius: 5px; margin: 20px 0; }
                    .alert-box { background-color: #fff3e0; border-left: 4px solid #ff9800; padding: 15px; margin: 10px 0; }
                    .footer { color: #999; font-size: 12px; text-align: center; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>⚠️ Atividade Suspeita Detectada</h2>
                    </div>
                    <div class="content">
                        <p>Uma atividade suspeita foi registrada em sua plataforma.</p>
                        
                        <div class="alert-box">
                            <p><strong>Usuário:</strong> %s</p>
                            <p><strong>Tipo de evento:</strong> %s</p>
                            <p><strong>Endereço IP:</strong> %s</p>
                            <p><strong>Detalhes:</strong> %s</p>
                            <p><strong>Data/Hora:</strong> %s</p>
                        </div>
                        
                        <p><strong>Próximos passos:</strong></p>
                        <ul>
                            <li>Verificar o painel de admin para mais detalhes</li>
                            <li>Revisar o histórico de auditoria do usuário</li>
                            <li>Tomar ações corretivas se necessário</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>Este é um email automático de segurança. Não responda a este email.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            vars.getOrDefault("userEmail", "N/A"),
            vars.getOrDefault("eventType", "N/A"),
            vars.getOrDefault("ipAddress", "N/A"),
            vars.getOrDefault("details", "N/A"),
            vars.getOrDefault("timestamp", "N/A")
        );
    }

    /**
     * Template para alerta de IP anômalo
     */
    private String renderAnomalousIpAlert(Map<String, Object> vars) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #c62828; color: white; padding: 20px; border-radius: 5px; }
                    .content { padding: 20px; background-color: #f5f5f5; border-radius: 5px; margin: 20px 0; }
                    .alert-box { background-color: #ffcdd2; border-left: 4px solid #c62828; padding: 15px; margin: 10px 0; }
                    .footer { color: #999; font-size: 12px; text-align: center; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🔴 Atividade Anômala de IP</h2>
                    </div>
                    <div class="content">
                        <p>Um IP com atividade anômala foi detectado.</p>
                        
                        <div class="alert-box">
                            <p><strong>Endereço IP:</strong> %s</p>
                            <p><strong>Total de eventos:</strong> %d</p>
                            <p><strong>Tentativas de login falhadas:</strong> %d</p>
                            <p><strong>Data/Hora:</strong> %s</p>
                        </div>
                        
                        <p><strong>Recomendações:</strong></p>
                        <ul>
                            <li>Bloquear o IP imediatamente se confirmado como malicioso</li>
                            <li>Investigar a origem e intenção do tráfego</li>
                            <li>Aumentar o monitoramento para este IP</li>
                            <li>Revisar todos os eventos associados no painel de admin</li>
                        </ul>
                    </div>
                    <div class="footer">
                        <p>Este é um email automático de segurança. Não responda a este email.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            vars.getOrDefault("ipAddress", "N/A"),
            vars.getOrDefault("eventCount", 0),
            vars.getOrDefault("failedLoginCount", 0),
            vars.getOrDefault("timestamp", "N/A")
        );
    }

    /**
     * Template para relatório de segurança diário
     */
    private String renderDailySecurityReport(Map<String, Object> vars) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #1976d2; color: white; padding: 20px; border-radius: 5px; }
                    .content { padding: 20px; background-color: #f5f5f5; border-radius: 5px; margin: 20px 0; }
                    .stats { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
                    .stat-box { background-color: #e3f2fd; padding: 15px; border-radius: 5px; text-align: center; }
                    .stat-number { font-size: 24px; font-weight: bold; color: #1976d2; }
                    .stat-label { color: #666; font-size: 12px; }
                    .footer { color: #999; font-size: 12px; text-align: center; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>📊 Relatório de Segurança Diário</h2>
                    </div>
                    <div class="content">
                        <p>Aqui está seu relatório de segurança do dia.</p>
                        
                        <div class="stats">
                            <div class="stat-box">
                                <div class="stat-number">%s</div>
                                <div class="stat-label">Total de Eventos</div>
                            </div>
                            <div class="stat-box">
                                <div class="stat-number">%s</div>
                                <div class="stat-label">Logins Bem-sucedidos</div>
                            </div>
                            <div class="stat-box">
                                <div class="stat-number">%s</div>
                                <div class="stat-label">Logins Falhados</div>
                            </div>
                            <div class="stat-box">
                                <div class="stat-number">%s</div>
                                <div class="stat-label">IPs Únicos</div>
                            </div>
                        </div>
                        
                        <p style="margin-top: 20px;"><strong>Recomendações:</strong></p>
                        <ul>
                            <li>Revise o painel de admin para análise detalhada</li>
                            <li>Investigue qualquer pico anormal de atividades</li>
                            <li>Atualize suas políticas de segurança conforme necessário</li>
                        </ul>
                        
                        <p style="text-align: center; margin-top: 20px;">
                            <a href="#" style="background-color: #1976d2; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
                                Ir para o Painel de Admin
                            </a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Relatório gerado em %s</p>
                        <p>Este é um email automático. Não responda a este email.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            vars.getOrDefault("totalEvents", "0"),
            vars.getOrDefault("successfulLogins", "0"),
            vars.getOrDefault("failedLogins", "0"),
            vars.getOrDefault("uniqueIps", "0"),
            vars.getOrDefault("generatedAt", "N/A")
        );
    }

    /**
     * Template para notificação de novo local
     */
    private String renderNewLocationNotification(Map<String, Object> vars) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #4caf50; color: white; padding: 20px; border-radius: 5px; }
                    .content { padding: 20px; background-color: #f5f5f5; border-radius: 5px; margin: 20px 0; }
                    .info-box { background-color: #e8f5e9; border-left: 4px solid #4caf50; padding: 15px; margin: 10px 0; }
                    .footer { color: #999; font-size: 12px; text-align: center; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>🔐 Novo Acesso Detectado</h2>
                    </div>
                    <div class="content">
                        <p>Detectamos um novo acesso em sua conta.</p>
                        
                        <div class="info-box">
                            <p><strong>Endereço IP:</strong> %s</p>
                            <p><strong>Localização aproximada:</strong> %s</p>
                            <p><strong>Data/Hora:</strong> %s</p>
                        </div>
                        
                        <p><strong>Se não foi você:</strong></p>
                        <ul>
                            <li>Altere sua senha imediatamente</li>
                            <li>Ative a autenticação de dois fatores</li>
                            <li>Revise a atividade recente em sua conta</li>
                            <li>Contate o suporte se verificar atividades suspeitas</li>
                        </ul>
                        
                        <p style="margin-top: 20px; text-align: center;">
                            <a href="#" style="background-color: #4caf50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;">
                                Revisar Segurança da Conta
                            </a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>Este é um email automático de segurança. Não responda a este email.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
            vars.getOrDefault("ipAddress", "N/A"),
            vars.getOrDefault("location", "Desconhecida"),
            vars.getOrDefault("timestamp", "N/A")
        );
    }

    /**
     * Template padrão para templates não encontrados
     */
    private String renderDefaultTemplate(String templateName, Map<String, Object> vars) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body>
                <p>Template '%s' não encontrado.</p>
            </body>
            </html>
            """, templateName
        );
    }
}
