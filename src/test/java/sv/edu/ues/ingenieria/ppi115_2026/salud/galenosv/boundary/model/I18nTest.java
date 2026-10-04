package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.boundary.model.AbstractModel.Mensajes;

class I18nTest {
    @Test
    void selectionSurvivesSessionSerializationAndRejectsUnsupportedLanguages() throws Exception {
        IdiomaBean bean = new IdiomaBean();
        assertEquals("es", bean.getIdioma());
        try (var context = org.mockito.Mockito.mockStatic(jakarta.faces.context.FacesContext.class)) {
            bean.setIdioma("zh");
        }
        bean.setIdioma("de");
        bean.setIdioma(null);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) { output.writeObject(bean); }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            IdiomaBean restored = (IdiomaBean) input.readObject();
            assertEquals(Locale.CHINESE, restored.getLocale());
        }
    }

    @Test
    void allLanguagesHaveEveryKeyWithoutRelyingOnFallback() throws Exception {
        assertEquals(load(""), load("_es"), "Los dos bundles españoles deben coincidir");
        Set<String> expected = load("").stringPropertyNames();
        for (String suffix : List.of("_es", "_en", "_fr", "_pt", "_zh")) {
            Properties messages = load(suffix);
            assertEquals(expected, messages.stringPropertyNames(), suffix);
            for (String key : expected) assertFalse(messages.getProperty(key).isBlank(), key);
        }
        assertEquals("语言", load("_zh").getProperty("idioma.label"));
        assertEquals("Sí", load("").getProperty("boton.si"));
    }

    @Test
    void spanishDoesNotFallBackToServerEnglish() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ENGLISH);
            ResourceBundle.clearCache();
            assertEquals("Guardar", ResourceBundle.getBundle("i18n.Messages", Locale.forLanguageTag("es")).getString("boton.guardar"));
        } finally {
            Locale.setDefault(previous);
            ResourceBundle.clearCache();
        }
    }

    @Test
    void selectionUpdatesTheCurrentViewAndDoesNotLeakIntoAnotherSession() {
        var faces = org.mockito.Mockito.mock(jakarta.faces.context.FacesContext.class);
        var view = org.mockito.Mockito.mock(jakarta.faces.component.UIViewRoot.class);
        org.mockito.Mockito.when(faces.getViewRoot()).thenReturn(view);
        try (var current = org.mockito.Mockito.mockStatic(jakarta.faces.context.FacesContext.class)) {
            current.when(jakarta.faces.context.FacesContext::getCurrentInstance).thenReturn(faces);
            var firstSession = new IdiomaBean();
            var secondSession = new IdiomaBean();
            for (String language : List.of("es", "en", "fr", "pt", "zh")) {
                firstSession.setIdioma(language);
                org.mockito.Mockito.verify(view).setLocale(Locale.forLanguageTag(language));
                assertEquals("es", secondSession.getIdioma());
            }
        }
    }

    @Test
    void javaMessagesUseTheFacesBundleAndServiceErrorKey() {
        var faces = org.mockito.Mockito.mock(jakarta.faces.context.FacesContext.class);
        var application = org.mockito.Mockito.mock(jakarta.faces.application.Application.class);
        org.mockito.Mockito.when(faces.getApplication()).thenReturn(application);
        try (var current = org.mockito.Mockito.mockStatic(jakarta.faces.context.FacesContext.class)) {
            current.when(jakarta.faces.context.FacesContext::getCurrentInstance).thenReturn(faces);
            for (String language : List.of("es", "en", "fr", "pt", "zh")) {
                var bundle = ResourceBundle.getBundle("i18n.Messages", Locale.forLanguageTag(language));
                org.mockito.Mockito.when(application.getResourceBundle(faces, "msg")).thenReturn(bundle);
                var error = new sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.control.service.ServiceException(
                        "error.duplicado", "Technical diagnostic", null);
                assertEquals(bundle.getString("error.duplicado"), Mensajes.detalle(error));
                assertEquals(bundle.getString("error.guardar"), Mensajes.texto("error.guardar"));
            }
        }
    }

    private Properties load(String suffix) throws Exception {
        Properties properties = new Properties();
        try (InputStream stream = getClass().getResourceAsStream("/i18n/Messages" + suffix + ".properties")) {
            assertNotNull(stream);
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
