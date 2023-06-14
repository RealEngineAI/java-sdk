package ai.realengine;

import ai.realengine.dto.ErrorDTO;

public class RealEngineAIException extends RuntimeException {

    /**
     * Unique error identifier, useful for support purposes
     */
    private final String errorId;

    /**
     * Error message
     */
    private final String errorMessage;

    /**
     * HTTP status code of the errored response
     */
    private final int httpStatus;

    /**
     * Path of the request that caused the error
     */
    private final String path;

    public RealEngineAIException(String message, int httpStatus, String path) {
        super(message + " http status: " + httpStatus + ", path: " + path);
        this.errorId = "";
        this.errorMessage = message;
        this.httpStatus = httpStatus;
        this.path = path;
    }

    public RealEngineAIException(ErrorDTO error, int httpStatus, String path) {
        super("Error id: " + error.getId() +
                ", message: " + error.getMsg() +
                ", http status: " + httpStatus +
                ", path: " + path);
        this.errorId = error.getId();
        this.errorMessage = error.getMsg();
        this.httpStatus = httpStatus;
        this.path = path;
    }

    public String getErrorId() {
        return errorId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getPath() {
        return path;
    }

}
