package ai.realengine.dto;

public class RealEngineAIResponse<T> {

    private boolean success;
    private T data;
    private ErrorDTO error;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ErrorDTO getError() {
        return error;
    }

    public void setError(ErrorDTO error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "RealEngineAIResponse{" +
                "success=" + success +
                ", data=" + data +
                ", error=" + error +
                '}';
    }
}