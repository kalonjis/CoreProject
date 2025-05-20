package be.steby.CoreProject.bll.common.services.context;

import be.steby.CoreProject.bll.common.models.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

public interface RequestContextService {

    RequestContext captureRequestContext(HttpServletRequest request);
}
