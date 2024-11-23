package com.spammers.AlertsAndNotifications.service.implementations;

import com.spammers.AlertsAndNotifications.model.LoanModel;
import com.spammers.AlertsAndNotifications.model.UserInfo;
import com.spammers.AlertsAndNotifications.repository.LoanRepository;
import com.spammers.AlertsAndNotifications.service.interfaces.EmailService;
import com.spammers.AlertsAndNotifications.service.interfaces.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LoanExpiredNotification {
    private final LoanRepository loanRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final ApiClient apiClient;

    private final int EXECUTIONS = 15;
    private int page = 0;


    @Scheduled(cron = "0 */10 8-10 * * MON-FRI") // Cada 10 minutos de lunes a viernes entre las 8 y las 10:59
    public void sendEmails() {
        processEmails();
        page = page > 17 ? 0 : page+1;
    }

    private void processEmails() {
        List<LoanModel> loans = fetchEmailsToSend();
        if(loans.isEmpty()){
            return;
        }
        for (LoanModel loan : loans) {

            sendEmail(loan);
        }
    }

    private List<LoanModel> fetchEmailsToSend() {
        Pageable pageable = PageRequest.of(page, EXECUTIONS, Sort.by("loanExpired").ascending());
        return loanRepository.findExpiredLoans(LocalDate.now(), pageable);
    }

    private void sendEmail(LoanModel loan) {
        UserInfo userInfo = apiClient.getUserInfoById(loan.getUserId());
        String emailBody = String.format("""
                Buen día .Su representado %s tomó prestado un libro en la fecha %s
                y a la fecha aún no lo ha devuelto. Solicitamos por favor se haga la entrega
                lo mas pronto posible.
                Gracias,
                Cordial Saludo.
                Este es el gestor de notificaciones de BibloSoft.
                No responder a esta cuenta de correo ya que es enviada por un motor de notificaciones automáticas."""
                , userInfo.getName(), loan.getLoanDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        emailService.sendEmailCustomised(userInfo.getGuardianEmail(), "Expiración préstamo libro", emailBody);
        changeLoanEmailExpiredSent(loan);
    }

    private void changeLoanEmailExpiredSent(LoanModel loanModel){
        loanModel.setEmailExpiredSent(true);
        loanRepository.save(loanModel);
    }
}
