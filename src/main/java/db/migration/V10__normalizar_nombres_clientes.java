package db.migration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Objects;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bienCriollas.stock.order.util.CustomerNameNormalizer;

/** Aplica a los pedidos históricos la misma regla usada por OrderService. */
public class V10__normalizar_nombres_clientes extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        try (Statement select = context.getConnection().createStatement();
                ResultSet orders = select.executeQuery(
                        "SELECT id_pedido, nombre_cliente FROM pedido");
                PreparedStatement update = context.getConnection().prepareStatement(
                        "UPDATE pedido SET nombre_cliente = ? WHERE id_pedido = ?")) {
            int pendingUpdates = 0;
            while (orders.next()) {
                String currentName = orders.getString("nombre_cliente");
                String normalizedName = CustomerNameNormalizer.normalize(currentName);
                if (!Objects.equals(currentName, normalizedName)) {
                    update.setString(1, normalizedName);
                    update.setLong(2, orders.getLong("id_pedido"));
                    update.addBatch();
                    pendingUpdates++;
                }
            }
            if (pendingUpdates > 0) {
                update.executeBatch();
            }
        }
    }
}
