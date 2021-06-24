package world.ucode;

import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.here.android.mpa.cluster.ClusterLayer;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.mapping.AndroidXMapFragment;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.CoreRouter;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;
import com.here.android.mpa.routing.Router;
import com.here.android.mpa.routing.RoutingError;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MapActivity {
    private final AppCompatActivity activity;
    private final AndroidXMapFragment mapFragment;
    private Map map;

    private GeoCoordinate[] geoCoordinates;
    private GeoPosition geoPosition;
    private NavigationManager navigationManager;
    private Route route;

    private final TextView statsLabel;
    private Button naviControlButton;

    private final StatManager statManager;

    public MapActivity(AppCompatActivity activity) {
        this.activity = activity;
        statsLabel = activity.findViewById(R.id.infolabel);
        mapFragment = (AndroidXMapFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
        statManager = new StatManager(activity);
    }

    public void initMap() {
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == Error.NONE) {
                    map = mapFragment.getMap();

                    assert map != null;
                    map.setCenter(new GeoCoordinate(51.475938,11.290319, 0.0),
                            Map.Animation.NONE);

                    map.setZoomLevel(13);
                    navigationManager = NavigationManager.getInstance();

                    geoCoordinates = new GeoCoordinate[] {
                            new GeoCoordinate(51.475938, 11.290319),
                            new GeoCoordinate(51.480986, 11.312334),
                            new GeoCoordinate(51.494013, 11.360745)
                    };

                    setConstMarkers();
                    initNaviControlButton();
                } else {
                    new AlertDialog.Builder(activity.getApplicationContext()).setMessage(
                            "Error : " + error.name() + "\n\n" + error.getDetails())
                            .setTitle(R.string.engine_init_error)
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(
                                                DialogInterface dialog,
                                                int which) {
                                            activity.finish();
                                        }
                                    }).create().show();
                }
            }
        });
    }

    public void setConstMarkers() {
        ArrayList<MapMarker> mapMarkers = new ArrayList<>();

        for (GeoCoordinate geo : geoCoordinates) {
            mapMarkers.add(new MapMarker(geo));
        }

        ClusterLayer clusterLayer = new ClusterLayer();
        clusterLayer.addMarkers(mapMarkers);
        map.addClusterLayer(clusterLayer);
    }

    private void setConstRoute() {
        CoreRouter coreRouter = new CoreRouter();
        RoutePlan routePlan = new RoutePlan();

        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setHighwaysAllowed(true);
        routeOptions.setRouteCount(3);
        routeOptions.setRouteType(RouteOptions.Type.SHORTEST);

        routePlan.setRouteOptions(routeOptions);

        coreRouter.calculateRoute(Arrays.asList(geoCoordinates.clone()), routeOptions,
                new Router.Listener<List<RouteResult>, RoutingError>() {
                    @Override
                    public void onProgress(int i) {
                        //
                    }

                    @Override
                    public void onCalculateRouteFinished(@NonNull List<RouteResult> routeResults,
                                                         @NonNull RoutingError routingError) {
                        if (routingError == RoutingError.NONE) {
                            routeResults.get(0).getRoute();
                            route = routeResults.get(0).getRoute();
                            map.addMapObject(new MapRoute(route));
                            startNavigation();
                        } else {
                            System.out.println("Rout calculate is not valid");
                        }
                    }
                });
    }

    private void startNavigation() {
        navigationManager.setMap(map);
        Objects.requireNonNull(mapFragment.getPositionIndicator()).setVisible(true);

        navigationManager.simulate(route, 60);
        map.setTilt(40);
        navigationManager.setMapUpdateMode(NavigationManager.MapUpdateMode.ROADVIEW);
        addNavigationListeners();
    }

    private void addNavigationListeners() {
        navigationManager.addPositionListener(
                new WeakReference<>(positionListener));

        navigationManager.addNavigationManagerEventListener(
                new WeakReference<>(navigationManagerEventListener));
    }

    private final NavigationManager.PositionListener positionListener
            = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(@NonNull GeoPosition geo) {
            if (route != null) {
                geoPosition = geo;
                statsLabel.setText(statManager.buildStat(geoPosition, route));
            }
        }
    };

    private final NavigationManager.NavigationManagerEventListener navigationManagerEventListener
            = new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onDestinationReached() {
            Toast.makeText(activity, "Travel was ended", Toast.LENGTH_SHORT).show();
            navigationManager.stop();
            naviControlButton.setText(R.string.restart_navi);
            statManager.saveStat(route, geoPosition);
            route = null;
        }
    };

    private void initNaviControlButton() {
        naviControlButton = activity.findViewById(R.id.naviCtrlButton);
        naviControlButton.setText(R.string.start_navi);
        naviControlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (route == null) {
                    setConstRoute();
                    naviControlButton.setText(R.string.stop_navi);
                } else {
                    navigationManager.stop();
                    naviControlButton.setText(R.string.restart_navi);
                    statManager.saveStat(route, geoPosition);
                    route = null;
                }
            }
        });
    }
}
