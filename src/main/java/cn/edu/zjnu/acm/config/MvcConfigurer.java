package cn.edu.zjnu.acm.config;

import cn.edu.zjnu.acm.interceptor.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class MvcConfigurer implements WebMvcConfigurer {
    
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // 设置字符串转换器使用UTF-8编码
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringConverter.setWriteAcceptCharset(false);
        converters.add(stringConverter);
        
        // JSON转换器默认使用UTF-8
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setDefaultCharset(StandardCharsets.UTF_8);
        converters.add(jsonConverter);
    }
    @Bean
    TeacherCheckInterceptor getTeacherCheckInterceptor() {
        return new TeacherCheckInterceptor();
    }

    @Bean
    LoginViewInterceptor getLoginViewInterceptor() {
        return new LoginViewInterceptor();
    }

    @Bean
    AdminCheckInterceptor getAdminCheckInterceptor() {
        return new AdminCheckInterceptor();
    }

    @Bean
    ContestInterceptor getContestInterceptor() {
        return new ContestInterceptor();
    }

    @Bean
    LoginInterceptor getLoginInterceptor() {
        return new LoginInterceptor();
    }

    @Bean
    TeamInterceptor getTeamInterceptor() {
        return new TeamInterceptor();
    }

    @Bean
    UnavailableInterceptor getUnavailableInterceptor() {
        return new UnavailableInterceptor();
    }

    @Bean
    IconInterceptor getIconInterceptor() {
        return new IconInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getIconInterceptor())
                .addPathPatterns("/favicon.ico")
                .addPathPatterns("/logo");
        registry.addInterceptor(getUnavailableInterceptor()).addPathPatterns("/**")
                .excludePathPatterns("/judge/callback");
        String[] needLogin = {"/contest/**", "/team/**", "/problems/**", "/user/**", "/admin/**", "/status/**", "/forum/**"};
        registry.addInterceptor(getLoginViewInterceptor())
                .addPathPatterns(needLogin);
        registry.addInterceptor(getTeacherCheckInterceptor())
                .addPathPatterns("/admin/**")
                .addPathPatterns("/contest/create/0")
                .addPathPatterns("/api/admin/**");
        registry.addInterceptor(getAdminCheckInterceptor())
                .addPathPatterns("/admin/settings")
                .addPathPatterns("/admin/user/**")
                .addPathPatterns("/admin/tag/**")
                .addPathPatterns("/api/admin/config")
                .addPathPatterns("/api/admin/correctData")
                .addPathPatterns("/api/admin/maintain")
                .addPathPatterns("/api/admin/user/**")
                .addPathPatterns("/api/admin/tag/**");
        registry.addInterceptor(getContestInterceptor())
                .addPathPatterns("/contest/*/**")
                .excludePathPatterns("/contest/*")
                .excludePathPatterns("/contest/problem/*")
                .excludePathPatterns("/contest/create/**")
                .excludePathPatterns("/contest/edit/**")
                .addPathPatterns("/api/contest/*/**")
                .excludePathPatterns("/api/contest/gate/*")
                .excludePathPatterns("/api/contest/*")
                .excludePathPatterns("/api/contest/background/**")
                .excludePathPatterns("/api/contest/clone/*");
        String[] apiNeedLogin = {
                "/api/problems/**",
                "/api/status/**",
                "/api/user/**",
                "/api/team/**",
                "/api/forum/**",
                "/api/admin/**",
                "/api/contest/**"
        };
        registry.addInterceptor(getLoginInterceptor())
                .addPathPatterns(apiNeedLogin)
                .addPathPatterns();
        registry.addInterceptor(getTeamInterceptor())
                .addPathPatterns("/team/manage/**")
                .addPathPatterns("/contest/create/*")
                .excludePathPatterns("/contest/create/0")
                .addPathPatterns("/api/team/**")
                .excludePathPatterns("/api/team")
                .excludePathPatterns("/api/team/*")
                .excludePathPatterns("/api/team/apply/*")
                .excludePathPatterns("/api/team/invite/**");
    }
}
