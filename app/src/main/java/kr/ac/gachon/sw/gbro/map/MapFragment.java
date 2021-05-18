package kr.ac.gachon.sw.gbro.map;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.CameraPosition;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.widget.LocationButtonView;

import java.util.ArrayList;
import java.util.Iterator;

import kr.ac.gachon.sw.gbro.R;
import kr.ac.gachon.sw.gbro.base.BaseFragment;
import kr.ac.gachon.sw.gbro.databinding.FragmentMapBinding;
import kr.ac.gachon.sw.gbro.util.Util;

public class MapFragment extends BaseFragment<FragmentMapBinding> implements OnMapReadyCallback {
    private ArrayList<Marker> markers = null;

    private com.naver.maps.map.MapFragment mapFragment;
    private ArrayList<Integer> path = null;
    private boolean isMain = false;

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

    @Override
    public void onResume() {
        super.onResume();
        mapFragment.getMapAsync(naverMap -> {
            if (path != null && !path.isEmpty()) {
                Log.d("MapFragment", "Path Map");
                String firstPos = getResources().getStringArray(R.array.gachon_globalcampus_coordinate)[path.get(0)];
                String[] posArray = firstPos.split(",");
                naverMap.setCameraPosition(new CameraPosition(new LatLng(Double.parseDouble(posArray[0]), Double.parseDouble(posArray[1])), 14.5));
                drawPath(naverMap, path);
            }
        });
    }

    private void setMap() {
        FragmentManager fragmentManager = getChildFragmentManager();
        mapFragment = (com.naver.maps.map.MapFragment) fragmentManager.findFragmentById(binding.map.getId());

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
                naverMap.setExtent(new LatLngBounds(new LatLng(37.44792028734633, 127.12628356183701), new LatLng(37.4570968690434, 127.13723061921826)));
                naverMap.setMinZoom(14.0);
                naverMap.setMaxZoom(0.0);

                if(isMain) {
                    Log.d("MapFragment", "Main Map");
                    drawMarker(naverMap);
                }
            });
        }
    }

    /**
     * 지도에 Marker를 표시한다
     * @author Subin Kim, Minjae Seon
     * @param naverMap NaverMap Object
     */
    public void drawMarker(NaverMap naverMap) {
        markers = new ArrayList<>();
        String[] buildingCoordinate = getResources().getStringArray(R.array.gachon_globalcampus_coordinate);

        for(int i = 0; i < buildingCoordinate.length - 1; i++) {
            String[] posArray = buildingCoordinate[i].split(",");

            Marker marker = new Marker();
            marker.setPosition(new LatLng(Double.parseDouble(posArray[0]), Double.parseDouble(posArray[1])));
            marker.setMap(naverMap);
            marker.setWidth(40);
            marker.setHeight(60);
            marker.setIcon(OverlayImage.fromResource(R.drawable.marker));
            setInfoWindow(marker, i);
            markers.add(marker);
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

    /**
     * Marker의 InfoWindow 설정
     * @author Subin Kim, Minjae Seon
     * @param marker Marker
     * @param buildingNum 건물 번호
     */
    private void setInfoWindow(Marker marker, int buildingNum) {
        String[] buildingName = getResources().getStringArray(R.array.gachon_globalcampus_building);

        InfoWindow infoWindow = new InfoWindow();
        marker.setOnClickListener(overlay -> {
            if (marker.getInfoWindow() == null) { // 마커를 클릭할 때 정보창을 엶
                // 다른 열린 마커 닫기
                closeOpenMarkers();
                infoWindow.open(marker);
            } else {
                // 이미 현재 마커에 정보 창이 열려있을 경우 닫음
                infoWindow.close();
            }
            return true;
        });

        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getContext()) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                // TODO : DB에서 각 건물 별 데이터 가져오기
                return getString(R.string.Info_building_name, buildingName[buildingNum])
                        +"\n"
                        +getString(R.string.Info_lost_cnt,3)
                        +"\n"
                        +getString(R.string.Info_get_cnt,4);
            }
        });
    }

    /**
     * 모든 열린 마커들을 닫음
     * @author Minjae Seon
     */
    private void closeOpenMarkers() {
        for (Marker marker : markers) {
            InfoWindow infoWindow = marker.getInfoWindow();
            if (infoWindow != null) {
                infoWindow.close();
            }
        }
    }
}
