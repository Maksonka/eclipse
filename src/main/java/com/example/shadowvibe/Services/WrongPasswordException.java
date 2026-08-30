package com.example.shadowvibe.Services;

/**
 * Бросается, когда указан неверный текущий пароль при смене пароля.
 * Отдельный тип нужен, чтобы отличать её от ошибок валидации нового пароля.
 */
public class WrongPasswordException extends RuntimeException {

    public WrongPasswordException(String message) {
        super(message);
    }
}
