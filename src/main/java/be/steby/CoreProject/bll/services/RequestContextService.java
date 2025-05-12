package be.steby.CoreProject.bll.services;

import be.steby.CoreProject.bll.models.RequestContext;
import jakarta.servlet.http.HttpServletRequest;

public interface RequestContextService {

    RequestContext captureRequestContext(HttpServletRequest request);
}
