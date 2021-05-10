package kr.ac.gachon.sw.gbro.map;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.firestore.GeoPoint;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapView;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;
import com.naver.maps.map.widget.LocationButtonView;

import java.util.ArrayList;
import java.util.Arrays;

import kr.ac.gachon.sw.gbro.R;
import kr.ac.gachon.sw.gbro.base.BaseFragment;
import kr.ac.gachon.sw.gbro.databinding.FragmentMapBinding;
import kr.ac.gachon.sw.gbro.util.Util;

public class MapFragment extends BaseFragment<FragmentMapBinding> implements OnMapReadyCallback {
    private ArrayList<Integer> path = null;
    private boolean isMain = false;
    private Context context;

    @Override
    protected FragmentMapBinding getBinding() {
        return FragmentMapBinding.inflate(getLayoutInflater());
    }

    public static MapFragment getPathInstance(ArrayList<Integer> path){
        MapFragment mapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putIntegerArrayList("path", path);
        mapFragment.setArguments(args);
        return mapFragment;
    }

    public static MapFragment getMainInstance() {
        MapFragment mapFragment = new MapFragment();
        Bundle args = new Bundle();
        args.putBoolean("isMain", true);
        mapFragment.setArguments(args);
        return mapFragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            path = getArguments().getIntegerArrayList("path");
            isMain = getArguments().getBoolean("isMain");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setMap();

        return getBinding().getRoot();
    }

    private void setMap() {
        FragmentManager fragmentManager = getChildFragmentManager();
        com.naver.maps.map.MapFragment mapFragment = (com.naver.maps.map.MapFragment) fragmentManager.findFragmentById(binding.map.getId());

        if(mapFragment == null) {
            NaverMapOptions options = new NaverMapOptions()
                    .camera(new CameraPosition(new LatLng(37.45199894842855, 127.13179114165393), 14))
                    .locationButtonEnabled(true)
                    .logoGravity(Gravity.START | Gravity.TOP)
                    .logoMargin(8, 8, 8, 8)
                    .locationButtonEnabled(false)
                    .compassEnabled(false)
                    .zoomControlEnabled(false);
            mapFragment = com.naver.maps.map.MapFragment.newInstance(options);
            fragmentManager.beginTransaction().add(binding.map.getId(), mapFragment).commit();

            mapFragment.getMapAsync(naverMap -> {
                LocationButtonView locationButtonView = binding.mapwidgetLocation;
                locationButtonView.setMap(naverMap);

                naverMap.setExtent(new LatLngBounds(new LatLng(37.44792028734633, 127.12628356183701), new LatLng(37.4570968690434, 127.13723061921826)));
                naverMap.setMinZoom(14.0);
                naverMap.setMaxZoom(0.0);

                if (path != null && !path.isEmpty()) {
                    drawPath(naverMap, path);
                }

                if(isMain) drawMarker(naverMap);
            });
        }
    }

    /**
     * 지도에 Marker를 표시한다
     * @author Subin Kim, Minjae Seon
     * @param naverMap NaverMap Object
     */
    public void drawMarker(NaverMap naverMap) {
        String[] buildingName = getResources().getStringArray(R.array.gachon_globalcampus_building);
        String[] buildingCoordinate = getResources().getStringArray(R.array.gachon_globalcampus_coordinate);

        for(int i = 0; i < buildingName.length - 1; i++) {
            String[] posArray = buildingCoordinate[i].split(",");

            Util.debugLog(getActivity(), "Maker Added : " + buildingName[i] + " (" + Double.parseDouble(posArray[0]) + ", " + Double.parseDouble(posArray[1]) + ")");

            Marker marker = new Marker();
            marker.setPosition(new LatLng(Double.parseDouble(posArray[0]), Double.parseDouble(posArray[1])));
            marker.setMap(naverMap);
            marker.setWidth(40);
            marker.setHeight(60);
            marker.setIcon(OverlayImage.fromResource(R.drawable.marker));

            InfoWindow infoWindow = new InfoWindow();
            marker.setOnClickListener(overlay -> {
                if (marker.getInfoWindow() == null) { // 마커를 클릭할 때 정보창을 엶
                    infoWindow.open(marker);
                } else {
                    // 이미 현재 마커에 정보 창이 열려있을 경우 닫음
                    infoWindow.close();
                }
                return true;
            });

            int currentBuilding = i;
            infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getContext()) {
                @NonNull
                @Override
                public CharSequence getText(@NonNull InfoWindow infoWindow) {
                    // TODO : DB에서 각 건물 별 데이터 가져오기
                    return getString(R.string.Info_building_name,buildingName[currentBuilding])
                            +"\n"
                            +getString(R.string.Info_get_cnt,3)
                            +"\n"
                            +getString(R.string.Info_get_cnt,4);
                }
            });
        }


    }

    /**
     * 지도에 Path를 표시한다
     * @author Suyeon Jung, Minjae Seon
     * @param naverMap NaverMap Object
     * @param pathArr 건물 번호 ArrayList
     */
    public void drawPath(NaverMap naverMap, ArrayList<Integer> pathArr){
        // Exception
        if(pathArr.size() > 5) {
            return;
        }

        String [] str_coordinate = getResources().getStringArray(R.array.gachon_globalcampus_coordinate);
        ArrayList<LatLng> coordinates = new ArrayList<>();

        for (int i : pathArr){
            String [] arr = str_coordinate[i].split(",");
            coordinates.add(new LatLng(Double.parseDouble(arr[0]), Double.parseDouble(arr[1])));
        }

        PathOverlay path = new PathOverlay();
        path.setCoords(coordinates);

        path.setMap(naverMap);
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
    }
}
