package com.hotel.reservas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Envío de correos. Si no hay servidor SMTP configurado (variable SPRING_MAIL_HOST), no se envía nada y el mensaje
 * se escribe en el log: SOLO sirve para desarrollo, porque el enlace de recuperación queda visible en el log.
 * El envío es asíncrono para que el tiempo de respuesta no revele si el correo existe.
 */
@Slf4j
@Service
public class CorreoService {

    private final ObjectProvider<JavaMailSender> remitente;
    private final String desde;

    public CorreoService(ObjectProvider<JavaMailSender> remitente, @Value("${correo.remitente:}") String desde) {
        this.remitente = remitente;
        this.desde = desde;
    }

    public void enviar(String para, String asunto, String texto) {
        JavaMailSender sender = remitente.getIfAvailable();
        if (sender == null) {
            log.warn("SMTP no configurado (SPRING_MAIL_HOST). Correo NO enviado (solo desarrollo). Para: {} | {} | {}",
                    para, asunto, texto);
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage m = new SimpleMailMessage();
                if (!desde.isBlank()) {
                    m.setFrom(desde);
                }
                m.setTo(para);
                m.setSubject(asunto);
                m.setText(texto);
                sender.send(m);
            } catch (Exception e) {
                log.error("No se pudo enviar el correo de '{}': {}", asunto, e.getMessage());
            }
        });
    }
}
