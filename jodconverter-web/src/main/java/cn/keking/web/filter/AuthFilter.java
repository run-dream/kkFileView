package cn.keking.web.filter;

import cn.keking.config.ConfigConstants;
import cn.keking.hutool.URLEncoder;
import fr.opensagres.xdocreport.document.json.JSONObject;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import javax.servlet.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * @author run-dream
 * @since 2020/12/15 17:00
 */
public class AuthFilter implements Filter {
    private String noAuth;

    @Override
    public void init(FilterConfig filterConfig) {
        ClassPathResource classPathResource = new ClassPathResource("web/noAuth.html");
        try {
            classPathResource.getInputStream();
            byte[] bytes = FileCopyUtils.copyToByteArray(classPathResource.getInputStream());
            this.noAuth = new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        if (!ConfigConstants.isAuthEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!this.validAuth(request)) {
            response.getWriter().write(this.noAuth);
            response.getWriter().close();
            return;
        }
        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }

    private boolean validAuth(ServletRequest request) {
        Map<String, String[]> map = request.getParameterMap();
        String authUrl = ConfigConstants.getAuthUrl();
        String successPath = ConfigConstants.getAuthSuccessPath();
        ArrayList<String> queries = new ArrayList<String>();
        URLEncoder encoder = URLEncoder.createDefault();
        try {
            map.forEach((key, value) -> {
                queries.add(key + "=" + String.join(",", value));
            });
            URL url = new URL(authUrl + "?" + encoder.encode(String.join("&", queries), Charset.forName("UTF-8")));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);

            connection.setReadTimeout(60000);
            connection.connect();
            if (connection.getResponseCode() != 200) {
                return false;
            }
            InputStream is = connection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            StringBuffer sbf = new StringBuffer();
            String temp = null;
            while ((temp = br.readLine()) != null) {
                sbf.append(temp);
                sbf.append("\r\n");
            }
            String result = sbf.toString();
            JSONObject jsonObj = new JSONObject(result);
            String[] paths = successPath.split("\\.");
            JSONObject deepObj = jsonObj;
            for (int i = 0; i < paths.length - 1; i++) {
                deepObj = deepObj.getJSONObject(paths[i]);
            }
            return deepObj.getBoolean(paths[paths.length - 1]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}