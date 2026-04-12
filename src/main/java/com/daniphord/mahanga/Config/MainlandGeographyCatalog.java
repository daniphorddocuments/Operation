package com.daniphord.mahanga.Config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class MainlandGeographyCatalog {

    private final Map<String, List<String>> mainlandGeography = buildMainlandGeography();

    public Map<String, List<String>> geography() {
        return mainlandGeography;
    }

    public List<String> districtsForRegion(String regionName) {
        return mainlandGeography.entrySet().stream()
                .filter(entry -> matches(entry.getKey(), regionName))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(List.of());
    }

    public boolean isMainlandRegion(String regionName) {
        return mainlandGeography.keySet().stream().anyMatch(name -> matches(name, regionName));
    }

    public boolean isMainlandDistrict(String regionName, String districtName) {
        return districtsForRegion(regionName).stream().anyMatch(name -> matches(name, districtName));
    }

    public String canonicalRegionName(String regionName) {
        return mainlandGeography.keySet().stream()
                .filter(name -> matches(name, regionName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Only Tanzania Mainland regions are supported"));
    }

    public String canonicalDistrictName(String regionName, String districtName) {
        return districtsForRegion(regionName).stream()
                .filter(name -> matches(name, districtName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("District must belong to the selected Tanzania Mainland region"));
    }

    public String canonicalStationName(String districtName) {
        if (districtName == null || districtName.isBlank()) {
            throw new IllegalArgumentException("District name is required");
        }
        return districtName.trim() + " Fire Station";
    }

    private boolean matches(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private Map<String, List<String>> buildMainlandGeography() {
        // Source: NBS 2022 PHC Administrative Units Population Distribution Report, Tanzania Mainland Volume 1B.
        LinkedHashMap<String, List<String>> geography = new LinkedHashMap<>();
        geography.put("Dodoma", List.of(
                "Kondoa District Council",
                "Kondoa Town Council",
                "Mpwapwa District Council",
                "Kongwa District Council",
                "Chamwino District Council",
                "Dodoma City Council",
                "Bahi District Council",
                "Chemba District Council"
        ));
        geography.put("Arusha", List.of(
                "Monduli District Council",
                "Meru District Council",
                "Arusha District Council",
                "Longido District Council",
                "Karatu District Council",
                "Ngorongoro District Council",
                "Arusha City Council"
        ));
        geography.put("Kilimanjaro", List.of(
                "Rombo District Council",
                "Mwanga District Council",
                "Same District Council",
                "Moshi Municipal Council",
                "Moshi District Council",
                "Hai District Council",
                "Siha District Council"
        ));
        geography.put("Tanga", List.of(
                "Lushoto District Council",
                "Bumbuli District Council",
                "Korogwe District Council",
                "Korogwe Town Council",
                "Muheza District Council",
                "Tanga City Council",
                "Pangani District Council",
                "Handeni District Council",
                "Handeni Town Council",
                "Kilindi District Council",
                "Mkinga District Council"
        ));
        geography.put("Morogoro", List.of(
                "Kilosa District Council",
                "Morogoro District Council",
                "Morogoro Municipal Council",
                "Mlimba District Council",
                "Ifakara Town Council",
                "Ulanga District Council",
                "Malinyi District Council",
                "Mvomero District Council",
                "Gairo District Council"
        ));
        geography.put("Pwani", List.of(
                "Bagamoyo District Council",
                "Chalinze District Council",
                "Kibaha District Council",
                "Kibaha Town Council",
                "Kisarawe District Council",
                "Mkuranga District Council",
                "Rufiji District Council",
                "Mafia District Council"
        ));
        geography.put("Dar es Salaam", List.of(
                "Kinondoni Municipal Council",
                "Dar es Salaam City Council",
                "Temeke Municipal Council",
                "Kigamboni Municipal Council",
                "Ubungo Municipal Council"
        ));
        geography.put("Lindi", List.of(
                "Kilwa District Council",
                "Mtama District Council",
                "Lindi Municipal Council",
                "Nachingwea District Council",
                "Liwale District Council",
                "Ruangwa District Council"
        ));
        geography.put("Mtwara", List.of(
                "Mtwara District Council",
                "Nanyamba Town Council",
                "Mtwara Municipal Council",
                "Newala District Council",
                "Newala Town Council",
                "Masasi District Council",
                "Masasi Town Council",
                "Tandahimba District Council",
                "Nanyumbu District Council"
        ));
        geography.put("Ruvuma", List.of(
                "Tunduru District Council",
                "Songea District Council",
                "Songea Municipal Council",
                "Madaba District Council",
                "Mbinga District Council",
                "Mbinga Town Council",
                "Nyasa District Council",
                "Namtumbo District Council"
        ));
        geography.put("Iringa", List.of(
                "Iringa District Council",
                "Iringa Municipal Council",
                "Mafinga Town Council",
                "Mufindi District Council",
                "Kilolo District Council"
        ));
        geography.put("Mbeya", List.of(
                "Chunya District Council",
                "Mbeya District Council",
                "Mbeya City Council",
                "Kyela District Council",
                "Rungwe District Council",
                "Busokelo District Council",
                "Mbarali District Council"
        ));
        geography.put("Singida", List.of(
                "Iramba District Council",
                "Singida District Council",
                "Singida Municipal Council",
                "Manyoni District Council",
                "Itigi District Council",
                "Ikungi District Council",
                "Mkalama District Council"
        ));
        geography.put("Tabora", List.of(
                "Nzega Town Council",
                "Nzega District Council",
                "Igunga District Council",
                "Uyui District Council",
                "Urambo District Council",
                "Sikonge District Council",
                "Tabora Municipal Council",
                "Kaliua District Council"
        ));
        geography.put("Rukwa", List.of(
                "Kalambo District Council",
                "Sumbawanga District Council",
                "Sumbawanga Municipal Council",
                "Nkasi District Council"
        ));
        geography.put("Kigoma", List.of(
                "Kibondo District Council",
                "Kasulu District Council",
                "Kasulu Town Council",
                "Kigoma District Council",
                "Kigoma Municipal Council",
                "Uvinza District Council",
                "Buhigwe District Council",
                "Kakonko District Council"
        ));
        geography.put("Shinyanga", List.of(
                "Ushetu District Council",
                "Kahama Municipal Council",
                "Msalala District Council",
                "Kishapu District Council",
                "Shinyanga District Council",
                "Shinyanga Municipal Council"
        ));
        geography.put("Kagera", List.of(
                "Karagwe District Council",
                "Bukoba District Council",
                "Bukoba Municipal Council",
                "Muleba District Council",
                "Biharamulo District Council",
                "Ngara District Council",
                "Kyerwa District Council",
                "Missenyi District Council"
        ));
        geography.put("Mwanza", List.of(
                "Ukerewe District Council",
                "Magu District Council",
                "Mwanza City Council",
                "Kwimba District Council",
                "Sengerema District Council",
                "Buchosa District Council",
                "Ilemela Municipal Council",
                "Misungwi District Council"
        ));
        geography.put("Mara", List.of(
                "Tarime District Council",
                "Tarime Town Council",
                "Serengeti District Council",
                "Musoma District Council",
                "Musoma Municipal Council",
                "Bunda District Council",
                "Bunda Town Council",
                "Butiama District Council",
                "Rorya District Council"
        ));
        geography.put("Manyara", List.of(
                "Babati District Council",
                "Babati Town Council",
                "Hanang District Council",
                "Mbulu District Council",
                "Mbulu Town Council",
                "Simanjiro District Council",
                "Kiteto District Council"
        ));
        geography.put("Njombe", List.of(
                "Njombe District Council",
                "Njombe Town Council",
                "Makambako Town Council",
                "Ludewa District Council",
                "Makete District Council",
                "Wanging'ombe District Council"
        ));
        geography.put("Katavi", List.of(
                "Mpanda Municipal Council",
                "Nsimbo District Council",
                "Tanganyika District Council",
                "Mlele District Council",
                "Mpimbwe District Council"
        ));
        geography.put("Simiyu", List.of(
                "Bariadi District Council",
                "Bariadi Town Council",
                "Itilima District Council",
                "Meatu District Council",
                "Maswa District Council",
                "Busega District Council"
        ));
        geography.put("Geita", List.of(
                "Geita District Council",
                "Geita Town Council",
                "Nyang'hwale District Council",
                "Mbogwe District Council",
                "Bukombe District Council",
                "Chato District Council"
        ));
        geography.put("Songwe", List.of(
                "Momba District Council",
                "Tunduma Town Council",
                "Songwe District Council",
                "Mbozi District Council",
                "Ileje District Council"
        ));
        return Collections.unmodifiableMap(geography);
    }
}
