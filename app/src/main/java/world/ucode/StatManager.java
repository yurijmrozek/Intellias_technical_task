package world.ucode;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import androidx.appcompat.app.AppCompatActivity;

import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.routing.Route;

import java.util.List;
import java.util.Objects;

public class StatManager {
    private final SQLiteDatabase database;

    public StatManager(AppCompatActivity activity) {
        database = new DatabaseHelper(activity).getWritableDatabase();
    }

    public String buildStat(GeoPosition geoPosition, Route route) {
        return "Speed: " + geoPosition.getSpeed() + "\n"
                + "Destination to endpoint: "

                + (int)geoPosition.getCoordinate().distanceTo
                    (Objects.requireNonNull(route.getDestination())) / 1000
                + " km "
                + (int)geoPosition.getCoordinate().distanceTo(route.getDestination()) % 1000
                + " m" + "\n"

                + "Route length: " + route.getLength() / 1000 + " km\n";
    }

    public void saveStat(Route route, GeoPosition geo) {
        if (route != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DatabaseHelper.ROUTE_DEST_SIZE, route.getLength());
            contentValues.put(DatabaseHelper.ROUTE_LENGTH,
                (int)geo.getCoordinate().distanceTo(Objects.requireNonNull(route.getDestination()))
                        / 1000);

            System.out.println(route.getLength() + "\n" +
                    (geo.getCoordinate().distanceTo(route.getDestination())));

            database.insert(DatabaseHelper.TABLE_NAME, null, contentValues);
            System.out.println(database.toString());
        }
    }
}
