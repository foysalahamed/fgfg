

import org.springframework.http.MediaType;

public interface ApiConstants {

    String EXTERNAL_MEDIA_TYPE = MediaType.APPLICATION_JSON_VALUE;

    String PRIVATE_API_ENDPOINT = "/leave/v1/api";

    String PRIVATE_IMAGE_UPLOAD_ENDPOINT = "/image";

    String PUBLIC_API_ENDPOINT = "/public";

    String ID = "id";

    String ID_PATH_VAR = "/{" + ID + "}";
}
