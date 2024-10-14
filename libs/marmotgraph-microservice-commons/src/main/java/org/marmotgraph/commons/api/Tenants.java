package org.marmotgraph.commons.api;

import org.marmotgraph.commons.model.tenant.ColorScheme;
import org.marmotgraph.commons.model.tenant.Font;
import org.marmotgraph.commons.model.tenant.ImageResult;
import org.marmotgraph.commons.model.tenant.TenantDefinition;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface Tenants {

    interface Client extends Tenants {}

    void createTenant(String name, TenantDefinition tenantDefinition);

    TenantDefinition getTenant(String name);

    List<String> listTenants();

    void setFont(String name, Font font);

    void setColorScheme(String name, ColorScheme colorScheme);

    void setCustomCSS(String name, String css);

    String getCSS(String name);

    ImageResult getFavicon(String name);

    void setFavicon(String name, MultipartFile file);

    ImageResult getBackgroundImage(String name, boolean darkMode);

    void setBackgroundImage(String name, MultipartFile file, boolean darkMode);

    ImageResult getLogo(String name, boolean darkMode);

    void setLogo(String name, MultipartFile file, boolean darkMode);
}
