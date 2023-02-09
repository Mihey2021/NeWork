package ru.netology.nework.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.*
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.mapview.MapView
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nework.R
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.databinding.FragmentMapBinding
import ru.netology.nework.dialogs.AppDialogs
import ru.netology.nework.dialogs.OnDialogsInteractionListener
import ru.netology.nework.models.Coordinates
import ru.netology.nework.utils.extensions.setIcon
import javax.inject.Inject

private lateinit var mapView: MapView
private lateinit var mapObjects: MapObjectCollection

@AndroidEntryPoint
class MapFragment : Fragment(R.layout.fragment_map), InputListener {

    private val args: MapFragmentArgs by navArgs()

    private var coordinates: Coordinates? = null
    private var readOnly: Boolean = false

    @Inject
    lateinit var appAuth: AppAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentMapBinding.inflate(inflater, container, false)

        MapKitFactory.initialize(requireContext())

        coordinates = args.coordinates
        readOnly = args.readOnly
        setActionBarTitle()

        mapView = binding.mapview
        mapView.map.addInputListener(this)

        mapObjects = mapView.map.mapObjects.addCollection()

        val userLocationBtn = binding.userLocationBtn
        userLocationBtn.setOnClickListener {
            onUserLocationClick()
        }

        val mapKit = MapKitFactory.getInstance()
        mapKit.resetLocationManagerToDefault()
        val userLocationLayer = mapKit.createUserLocationLayer(mapView.mapWindow)
        userLocationLayer.isVisible = true
        userLocationLayer.isHeadingEnabled = true

        if (coordinates != null) {
            addMarker(coordinates!!)
            moveMapCamera(Point(coordinates!!.lat.toDouble(), coordinates!!.long.toDouble()))
        }

        if (!readOnly) {
            requireActivity().addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.editing_menu, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {
                        R.id.save -> {
                            parentFragmentManager.setFragmentResult(
                                REQUEST_CODE, bundleOf(
                                    EXTRA_COORDINATES to coordinates
                                )
                            )
                            findNavController().navigateUp()
                            true
                        }
                        R.id.logout -> {
                            showLogoutQuestionDialog()
                            true
                        }
                        else -> false
                    }
            }, viewLifecycleOwner)
        }
        return binding.root
    }

    private fun setActionBarTitle() {
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.title =
            if (readOnly) getString(R.string.location) else getString(R.string.set_location)
    }

    private fun onUserLocationClick() {
        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        when {
            // 1. Проверяем есть ли уже права
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {

                val fusedLocationProviderClient = LocationServices
                    .getFusedLocationProviderClient(requireActivity())

                fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                    if (location == null) {
                        showLocationPermissionDialog(
                            getString(R.string.err_current_geo),
                            false
                        )
                        return@addOnSuccessListener
                    }
                    moveMapCamera(Point(location.latitude, location.longitude))
                }
            }
            // 2. Должны показать обоснование необходимости прав
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showLocationPermissionDialog(
                    getString(R.string.request_permission_geo_rationale),
                    true
                )
            }
            // 3. Запрашиваем права
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun showLocationPermissionDialog(message: String, retry: Boolean = false) {

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.access_to_geo))
            .setMessage(message)
            .setPositiveButton(getString(android.R.string.ok)) { dialog, _ ->
                if (retry) requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                dialog.dismiss()
            }
            .show()

    }

    private fun moveMapCamera(coordinates: Point) {
        mapView.map.move(
            CameraPosition(coordinates, 15f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 2f),
            null
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted)
                showLocationPermissionDialog(getString(R.string.check_geo_permission))
        }

    override fun onMapLongTap(map: Map, selectcoordinates: Point) {
        if (readOnly) return
        clearMarkers()
        val latitude = selectcoordinates.latitude.toString().substring(0, 9)
        val longitude = selectcoordinates.longitude.toString().substring(0, 9)
        coordinates = Coordinates(latitude, longitude)
        addMarker(coordinates!!)
    }

    override fun onMapTap(map: Map, coordinates: Point) {}

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun addMarker(marker: Coordinates) {
        val coordinates = Point(marker.lat.toDouble(), marker.long.toDouble())
        mapObjects.addPlacemark(coordinates).apply {
            opacity = 0.9f
            userData = marker
            setIcon(getDrawable(requireContext(), R.drawable.ic_baseline_user_marker)!!)
            addTapListener(markerTapListener)
        }
    }

    private val markerTapListener = MapObjectTapListener { mapObject, _ ->
        if (mapObject is PlacemarkMapObject) {
            val userMarker = mapObject.userData
            if (userMarker is Coordinates) {
                if (!readOnly) clearMarkers()
            }
        }
        true
    }

    private fun clearMarkers() {
        mapObjects.clear()
        coordinates = null
    }

    private fun showLogoutQuestionDialog() {
        AppDialogs.getDialog(requireContext(), AppDialogs.QUESTION_DIALOG,
            title = getString(R.string.logout),
            message = getString(R.string.do_you_really_want_to_get_out),
            titleIcon = R.drawable.ic_baseline_logout_24,
            positiveButtonTitle = getString(R.string.yes_text),
            onDialogsInteractionListener = object : OnDialogsInteractionListener {
                override fun onPositiveClickButton() {
                    appAuth.removeAuth()
                    findNavController().popBackStack(R.id.feedFragment, false)
                }
            })
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    override fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    companion object {
        const val REQUEST_CODE = "REQUEST_CODE"
        const val EXTRA_COORDINATES = "EXTRA_COORDINATES"
    }

}