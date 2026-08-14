package com.smartbudget.infrastructure.web.exception;

import com.smartbudget.domain.exception.InvalidPeriodException;
import com.smartbudget.domain.exception.InvalidTransactionException;
import com.smartbudget.domain.exception.TransactionNotFoundException;
import com.smartbudget.infrastructure.ai.exception.AiAssistantException;
import com.smartbudget.infrastructure.ai.exception.InvalidAudioException;
import com.smartbudget.infrastructure.ai.exception.SpeechSynthesisException;
import com.smartbudget.infrastructure.ai.exception.TranscriptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiError.FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Os dados enviados sao invalidos. Verifique os campos informados.", violations);
    }

    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiError> handleInvalidTransaction(InvalidTransactionException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION", exception.getMessage());
    }

    @ExceptionHandler(InvalidPeriodException.class)
    public ResponseEntity<ApiError> handleInvalidPeriod(InvalidPeriodException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PERIOD", exception.getMessage());
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TransactionNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidAudioException.class)
    public ResponseEntity<ApiError> handleInvalidAudio(InvalidAudioException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_AUDIO", exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return build(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE",
                "O arquivo enviado excede o tamanho maximo permitido.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException exception) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_FILE",
                "O arquivo e obrigatorio. Envie-o no campo '" + exception.getRequestPartName() + "'.");
    }

    @ExceptionHandler(TranscriptionException.class)
    public ResponseEntity<ApiError> handleTranscription(TranscriptionException exception) {
        log.error("Audio transcription failed", exception);
        return build(HttpStatus.BAD_GATEWAY, "TRANSCRIPTION_ERROR",
                "Nao foi possivel transcrever o audio enviado. Tente novamente em instantes.");
    }

    @ExceptionHandler(SpeechSynthesisException.class)
    public ResponseEntity<ApiError> handleSpeech(SpeechSynthesisException exception) {
        log.error("Speech synthesis failed", exception);
        return build(HttpStatus.BAD_GATEWAY, "SPEECH_ERROR",
                "Nao foi possivel gerar o audio de resposta. Tente novamente em instantes.");
    }

    @ExceptionHandler(AiAssistantException.class)
    public ResponseEntity<ApiError> handleAi(AiAssistantException exception) {
        log.error("AI assistant request failed", exception);
        return build(HttpStatus.BAD_GATEWAY, "AI_SERVICE_ERROR",
                "O assistente esta indisponivel no momento. Tente novamente em instantes.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "O corpo da requisicao esta ausente ou mal formatado.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Valor invalido para o parametro '" + exception.getName() + "'.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParameter(MissingServletRequestParameterException exception) {
        return build(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "O parametro '" + exception.getParameterName() + "' e obrigatorio.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Recurso nao encontrado: " + exception.getResourcePath());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "Recurso nao encontrado: " + exception.getRequestURL());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
                "O metodo " + exception.getMethod() + " nao e suportado neste endpoint.");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
                "Tipo de conteudo nao suportado neste endpoint.");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return build(HttpStatus.NOT_ACCEPTABLE, "NOT_ACCEPTABLE",
                "Nenhum dos formatos aceitos pelo cliente pode ser produzido por este endpoint.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocorreu um erro inesperado ao processar a requisicao.");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message) {
        return build(status, error, message, null);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message,
                                           List<ApiError.FieldViolation> details) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), error, message, details));
    }
}
