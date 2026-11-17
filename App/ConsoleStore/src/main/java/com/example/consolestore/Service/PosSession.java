package com.example.consolestore.Service;

import com.example.consolestore.Dto.PosData.Quote;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

@Component
@SessionScope
public class PosSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private String owner;
    private Quote quote;
    private boolean submitted;

    public synchronized <T> T locked(
            String username,
            Supplier<T> action
    ) {
        if (!Objects.equals(owner, username)) {
            owner = username;
            quote = null;
            submitted = false;
        }

        return action.get();
    }

    private void guard() {
        if (!Thread.holdsLock(this)) {
            throw new IllegalStateException(
                    "Use PosSession.locked()"
            );
        }
    }

    public Quote quote() {
        guard();
        return quote;
    }

    public boolean submitted() {
        guard();
        return submitted;
    }

    public void editable() {
        guard();

        if (submitted) {
            throw new PosRejectedException(
                    "Yêu cầu trước chưa rõ kết quả. "
                            + "Hãy quay lại xác nhận."
            );
        }
    }

    public void start(Quote value) {
        editable();
        quote = value;
    }

    public void markSubmitted() {
        guard();
        submitted = true;
    }

    public void rejected() {
        guard();
        submitted = false;
    }

    public void complete() {
        guard();
        quote = null;
        submitted = false;
    }
}