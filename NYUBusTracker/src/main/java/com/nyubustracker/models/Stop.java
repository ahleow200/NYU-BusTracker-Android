package com.nyubustracker.models;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.nyubustracker.BuildConfig;
import com.nyubustracker.activities.MainActivity;
import com.nyubustracker.helpers.BusManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Stop implements Comparable<Stop> {
    public static final String FAVORITES_PREF = "favorites";
    private final ArrayList<Stop> childStops;
    private String name;
    private String id;
    private LatLng loc;
    private String[] routesString;
    private ArrayList<Route> routes = null;
    private String otherRoute = null;
    private Map<String, List<Time>> times = null;
    private boolean favorite;
    private Stop parent;
    private Stop oppositeStop;
    private boolean hidden;

    public Stop(String mName, String mLat, String mLng, String mID, String[] mRoutes) {
        name = cleanName(mName);
        loc = new LatLng(Double.parseDouble(mLat), Double.parseDouble(mLng));
        id = mID;
        routesString = mRoutes;
        times = new HashMap<>();
        routes = new ArrayList<>();
        otherRoute = "";
        childStops = new ArrayList<>();
        BusManager sharedManager = BusManager.getBusManager();
        for (String s : mRoutes) {
            Route r = sharedManager.getRouteByID(s);
            if (r != null && !r.getStops().contains(this)) r.addStop(this);
        }
    }

    public static String cleanName(String name) {
        name = name.replaceAll("at", "@");
        name = name.replaceAll("[Aa]venue", "Ave");
        name = name.replaceAll("bound", "");
        name = name.replaceAll("[Ss]treet", "St");
        return name;
    }

    private static int compareStartingNumbers(String stop, String stop2) {
        int stopN = getStartingNumber(stop);
        int stopN2 = getStartingNumber(stop2);
        if (stopN > -1 && stopN2 > -1) return Integer.signum(stopN - stopN2);
        if (stopN > -1) return -1;
        if (stopN2 > -1) return 1;
        return Integer.signum(stopN - stopN2);
    }

    private static int getStartingNumber(String s) {
        if (Character.isDigit(s.charAt(0))) {
            int n = 0;
            while (n < s.length() && Character.isDigit(s.charAt(n))) {
                n++;
            }
            return Integer.parseInt(s.substring(0, n));
        } else return -1;
    }

    public static void parseJSON(JSONObject stopsJson) throws JSONException {
        JSONArray jStops = new JSONArray();
        BusManager sharedManager = BusManager.getBusManager();
        if (stopsJson != null) jStops = stopsJson.getJSONArray(BusManager.TAG_DATA);
        if (BuildConfig.DEBUG)
            Log.v(MainActivity.REFACTOR_LOG_TAG, "BusManager current # stops: " + sharedManager.getStops());
        if (BuildConfig.DEBUG)
            Log.v(MainActivity.REFACTOR_LOG_TAG, "Parsing # stops: " + jStops.length());
        for (int i = 0; i < jStops.length(); i++) {
            JSONObject stopObject = jStops.getJSONObject(i);
            String stopID = stopObject.getString(BusManager.TAG_STOP_ID);
            String stopName = stopObject.getString(BusManager.TAG_STOP_NAME);
            if (BuildConfig.DEBUG)
                Log.v(MainActivity.REFACTOR_LOG_TAG, "*   Stop: " + stopID + " | " + stopName);
            JSONObject location = stopObject.getJSONObject(BusManager.TAG_LOCATION);
            String stopLat = location.getString(BusManager.TAG_LAT);
            String stopLng = location.getString(BusManager.TAG_LNG);
            JSONArray stopRoutes = stopObject.getJSONArray(BusManager.TAG_ROUTES);
            String[] routes = new String[stopRoutes.length()];
            for (int j = 0; j < stopRoutes.length(); j++) {
                routes[j] = stopRoutes.getString(j);
            }
            Stop s = sharedManager.getStop(stopName, stopLat, stopLng, stopID, routes); // Creates the stop.
            //if (BuildConfig.DEBUG) Log.v(MainActivity.REFACTOR_LOG_TAG, "Number of stops in manager: " + sharedManager.numStops());
            //if (BuildConfig.DEBUG) Log.v(MainActivity.REFACTOR_LOG_TAG, "___after adding " + s.name);
        }
    }

    @Override
    public int compareTo(@NonNull Stop stop2) {
        if (this.getFavorite()) {
            if (stop2.getFavorite()) {
                return compareStartingNumbers(this.getName(), stop2.getName());
            } else return -1;
        } else if (stop2.getFavorite()) return 1;
        else return compareStartingNumbers(this.getName(), stop2.getName());
    }

    public Stop getOppositeStop() {
        return oppositeStop;
    }

    public void setOppositeStop(Stop stop) {
        oppositeStop = stop;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setOtherRoute(String r) {
        otherRoute = r;
    }

    public void setParentStop(Stop parent) {
        this.parent = parent;
    }

    public Stop getUltimateParent() {
        Stop result = this;
        while (result.getParent() != null) {
            result = result.getParent();
        }
        return result;
    }

    private Stop getParent() {
        return parent;
    }

    public void addChildStop(Stop stop) {
        if (!childStops.contains(stop)) {
            childStops.add(stop);
        }
    }

    /**
     * Retrieves a list of related Stop objects forming a family.
     * 
     * This method collects and returns an ArrayList of Stop objects that are related
     * to the current Stop. The family includes:
     * - All child stops
     * - The parent stop (if it exists)
     * - The opposite stop of the parent (if it exists)
     * - The opposite stop of the current stop (if it exists)
     * - The current stop itself
     * 
     * @return An ArrayList containing all the Stop objects in the family
     */
    public ArrayList<Stop> getFamily() {
        ArrayList<Stop> result = new ArrayList<>(childStops);
        if (parent != null) {
            result.add(parent);
            if (parent.oppositeStop != null) {
                result.add(parent.oppositeStop);
            }
        }
        if (oppositeStop != null) {
            result.add(oppositeStop);
        }
        result.add(this);
        return result;
    }

    private ArrayList<Stop> getChildStops() {
        return childStops;
    }

    /**
     * Sets various values for a bus stop, including name, location, ID, and associated routes.
     * This method also updates the BusManager with the new stop information.
     * 
     * @param mName The name of the bus stop
     * @param mLat The latitude of the bus stop's location
     * @param mLng The longitude of the bus stop's location
     * @param mID The unique identifier for the bus stop
     * @param mRoutes An array of route IDs that this bus stop is part of
     */
    public void setValues(String mName, String mLat, String mLng, String mID, String[] mRoutes) {
        if (name.equals("")) name = cleanName(mName);
        if (loc == null) loc = new LatLng(Double.parseDouble(mLat), Double.parseDouble(mLng));
        id = mID;
        if (routesString == null) routesString = mRoutes;
        BusManager sharedManager = BusManager.getBusManager();
        for (String s : mRoutes) {
            Route r = sharedManager.getRouteByID(s);
            if (r != null && !r.getStops().contains(this)) r.addStop(this);
        }
    }

    public LatLng getLocation() {
        return loc;
    }

    public String toString() {
        return name + " " + getID();
    }

    public boolean getFavorite() {
        return favorite;
    }

    public void setFavorite(boolean checked) {
        favorite = checked;
    }

    public boolean hasRouteByString(String routeID) {
        for (String route : routesString) {
            if (route.equals(routeID)) return true;
        }
        return false;
    }

    /**
     * Checks if a route with the given name exists in the list of routes.
     * 
     * @param name The name of the route to search for
     * @return true if a route with the given name is found, false otherwise
     */
    public boolean hasRouteByName(String name) {
        if (name.trim().isEmpty()) return false;
        for (Route r : routes) {
            if (r.getLongName().equals(name)) return true;
        }
        return false;
    }

    /**
     * Retrieves all routes associated with this stop, including routes from child stops,
     * parent's child stops (excluding this stop), and the opposite stop (if any).
     * 
     * @return An ArrayList of Route objects containing all unique routes associated
     *         with this stop and its related stops.
     */
    public ArrayList<Route> getRoutes() {
        ArrayList<Route> result = new ArrayList<>(routes);
        for (Stop child : childStops) {
            for (Route childRoute : child.getRoutes()) {
                if (!result.contains(childRoute)) {
                    result.add(childRoute);
                }
            }
        }
        if (parent != null) {
            for (Stop child : parent.getChildStops()) {
                if (child != this) {
                    for (Route childRoute : child.getRoutes()) {
                        if (!result.contains(childRoute)) {
                            result.add(childRoute);
                        }
                    }
                }
            }
        }
        if (oppositeStop != null) {
            for (Route r : oppositeStop.routes) {
                if (!result.contains(r)) {
                    result.add(r);
                }
            }
            for (Stop child : oppositeStop.getChildStops()) {
                if (child != this) {
                    for (Route childRoute : child.getRoutes()) {
                        if (!result.contains(childRoute)) {
                            result.add(childRoute);
                        }
                    }
                }
            }
        }
        return result;
    }

    public void addRoute(Route route) {
        routes.add(route);
    }

    public String getID() {
        return id;
    }

    /**
     * Adds a Time object to the times collection, organizing times by route.
     * If the route doesn't exist in the collection, it creates a new ArrayList
     * for that route before adding the Time object.
     * 
     * @param t The Time object to be added to the collection
     */
    public void addTime(Time t) {
        if (times.get(t.getRoute()) == null) times.put(t.getRoute(), new ArrayList<Time>());
        times.get(t.getRoute()).add(t);
    }

    public Map<String, List<Time>> getTimes() {
        return times;
    }

    /**
     * Retrieves a list of times for a specified route, including times from child stops.
     * 
     * @param route The route identifier for which to retrieve times
     * @return A list of Time objects representing all times for the specified route,
     *         including times from child stops
     */
    public List<Time> getTimesOfRoute(String route) {
        List<Time> result = times.get(route);
        for (Stop childStop : childStops) {
            result.addAll(childStop.getTimesOfRoute(route));
        }
        return result;
    }

    public boolean isRelatedTo(Stop stop) {
        return (this.getUltimateName().equals(stop.getUltimateName()));
    }

    public String getUltimateName() {
        return getUltimateParent().getName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieves a list of routes connecting the current stop to the specified end stop.
     * 
     * This method finds all available routes that start from the current stop and reach the given end stop.
     * It compares the routes available at both the start and end stops to determine the connecting routes.
     * 
     * @param endStop The destination stop to which routes are being searched
     * @return A list of Route objects representing the available routes connecting the current stop to the end stop
     */
    public List<Route> getRoutesTo(Stop endStop) {
        Stop startStop = this;
        ArrayList<Route> startRoutes = startStop.getUltimateParent().getRoutes();        // All the routes leaving the start stop.
        ArrayList<Route> endRoutes = endStop.getUltimateParent().getRoutes();
        ArrayList<Route> availableRoutes = new ArrayList<>();               // All the routes connecting the two.
        for (Route r : startRoutes) {
            if (endRoutes.contains(r) && !availableRoutes.contains(r)) {
                availableRoutes.add(r);
            }
        }
        return availableRoutes;
    }

    /**
     * Retrieves a sorted list of times to reach the specified end stop.
     * 
     * This method compares the routes of the current stop with the routes of the end stop,
     * and collects all the times for matching routes. The resulting list is then sorted.
     * 
     * @param endStop The destination stop to which times are calculated
     * @return A sorted List of Time objects representing the times to reach the end stop
     */
    public List<Time> getTimesTo(Stop endStop) {
        List<Route> startRoutes = getRoutes();
        List<Route> endRoutes = endStop.getRoutes();
        List<Time> result = new ArrayList<>();
        for (Route r : startRoutes) {
            if (endRoutes.contains(r) && times.containsKey(r.getLongName())) {
                result.addAll(times.get(r.getLongName()));
            }
        }
        Collections.sort(result);
        return result;
    }

    /**
     * Checks if this stop is connected to the given stop.
     * 
     * @param stop The stop to check for connection
     * @return true if there is at least one route connecting this stop to the given stop, false otherwise
     */
    public boolean isConnectedTo(Stop stop) {
        return stop != null && !this.getRoutesTo(stop).isEmpty();
    }

    /**
     * Calculates the number of stops between this stop and the given stop along their shared route.
     * 
     * @param stop The destination stop to calculate the distance to
     * @return The number of stops between this stop and the given stop, or Integer.MAX_VALUE if there is no shared route or if the input is null
     */
    public int distanceTo(Stop stop) {
        if (stop == null) return Integer.MAX_VALUE;
        List<Route> routes = getRoutesTo(stop);
        if (routes.isEmpty()) return Integer.MAX_VALUE;
        else {
            Route r = routes.get(0);
            List<Stop> stops = r.getStops();
            for (int i = 0; i < stops.size(); i++) {
                if (stops.get(i).getID().equals(getID())) {
                    for (int j = 1; ((i + j) % stops.size()) != i; j++) {
                        if (stops.get((i + j) % stops.size()).getID().equals(stop.getID())) {
                            return j;
                        }
                    }
                }
            }
        }
        return Integer.MAX_VALUE;
    }
}
