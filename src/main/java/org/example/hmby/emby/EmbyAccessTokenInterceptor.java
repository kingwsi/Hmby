package org.example.hmby.emby;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.example.hmby.exception.BusinessException;
import org.example.hmby.sceurity.EmbyUser;
import org.example.hmby.sceurity.SecurityUtils;

public class EmbyAccessTokenInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        EmbyUser userDetails = SecurityUtils.getUserInfo()
                .orElseThrow(() -> new BusinessException("emby认证信息获取异常！"));
        String url = template.url().replace("USER_ID", userDetails.getUserId());
        template.header("X-Emby-Token", userDetails.getThirdPartyToken());
        template.header("Accept", "*/*");
        
        template.uri(url);
    }
}
