package sv.edu.ues.ingenieria.ppi115_2026.salud.galenosv.entity;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RolTest {
    @Test void calculaCodigosDeLaApiSinColumnaAdicional() {
        String[][] casos = {{"Paciente", "PACIENTE"}, {" PACIENTE ", "PACIENTE"},
                {"Atención al público", "ADMINISTRACION"}, {"Administración", "ADMINISTRACION"},
                {"Doctor", "DOCTOR"}, {"Enfermera", "ENFERMERA"}, {"Farmacéutico", "FARMACEUTICO"},
                {"Laboratorista", null}, {"Paciente externo", null}, {"Secretaría", null}, {null, null}};
        for (String[] caso : casos) {
            Rol rol = new Rol(); rol.setNombre(caso[0]);
            assertEquals(caso[1], rol.getCodigo(), caso[0]);
        }
    }
}
