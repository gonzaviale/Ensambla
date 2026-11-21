param(
    [Parameter(Mandatory = $true)]
    [string]$SalidaSql,
    [string]$Endpoint = 'https://overpass.openstreetmap.fr/api/interpreter',
    [datetime]$Fecha = (Get-Date)
)

$ErrorActionPreference = 'Stop'

$zonas = @(
    @{ Nombre = 'Almagro'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.6087; Lon = -58.4195 },
    @{ Nombre = 'Boedo'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.6290; Lon = -58.4190 },
    @{ Nombre = 'Caballito Norte'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.6120; Lon = -58.4350 },
    @{ Nombre = 'Caballito Sur'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.6230; Lon = -58.4410 },
    @{ Nombre = 'Chacarita'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.5872; Lon = -58.4542 },
    @{ Nombre = 'Colegiales'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.5748; Lon = -58.4497 },
    @{ Nombre = 'Villa Ortúzar'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.5797; Lon = -58.4686 },
    @{ Nombre = 'Paternal'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.5964; Lon = -58.4701 },
    @{ Nombre = 'Villa Crespo Sur'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.5996; Lon = -58.4403 },
    @{ Nombre = 'Villa del Parque'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.6038; Lon = -58.4930 },
    @{ Nombre = 'Flores Norte'; Localidad = 'Ciudad Autónoma de Buenos Aires'; Partido = 'CABA'; Provincia = 'CABA'; Lat = -34.6272; Lon = -58.4638 },
    @{ Nombre = 'Ramos Mejía'; Localidad = 'Ramos Mejía'; Partido = 'La Matanza'; Provincia = 'Buenos Aires'; Lat = -34.6419; Lon = -58.5666 },
    @{ Nombre = 'San Justo - UNLaM'; Localidad = 'San Justo'; Partido = 'La Matanza'; Provincia = 'Buenos Aires'; Lat = -34.6703; Lon = -58.5628 }
)

function Sql([object]$valor) {
    if ($null -eq $valor) { return 'NULL' }
    return "'" + ([string]$valor).Replace("'", "''") + "'"
}

function Numero([double]$valor) {
    return $valor.ToString('0.######', [Globalization.CultureInfo]::InvariantCulture)
}

function Coordenadas($elemento) {
    if ($null -ne $elemento.lat -and $null -ne $elemento.lon) {
        return @{ Lat = [double]$elemento.lat; Lon = [double]$elemento.lon }
    }
    if ($null -ne $elemento.center) {
        return @{ Lat = [double]$elemento.center.lat; Lon = [double]$elemento.center.lon }
    }
    return $null
}

function Clasificaciones($tags) {
    $resultado = [System.Collections.Generic.List[object]]::new()
    $amenity = [string]$tags.amenity
    $shop = [string]$tags.shop
    $leisure = [string]$tags.leisure
    $publicTransport = [string]$tags.public_transport
    $highway = [string]$tags.highway
    $railway = [string]$tags.railway

    if ($amenity -in @('school', 'kindergarten', 'college', 'university')) {
        $resultado.Add(@{ Tipo = 'ESCUELA'; Rubro = $null })
    }
    if ($null -ne $tags.office) {
        $resultado.Add(@{ Tipo = 'OFICINA'; Rubro = $null })
    }
    if ($publicTransport -or $highway -eq 'bus_stop' -or $railway -in @('station', 'halt', 'subway_entrance', 'tram_stop')) {
        $resultado.Add(@{ Tipo = 'TRANSPORTE'; Rubro = $null })
    }
    if ($leisure -in @('park', 'garden', 'playground')) {
        $resultado.Add(@{ Tipo = 'PARQUE'; Rubro = $null })
    }
    if ($amenity -in @('hospital', 'clinic', 'doctors')) {
        $resultado.Add(@{ Tipo = 'SALUD'; Rubro = $null })
    }
    if ($amenity -in @('pharmacy', 'bank', 'atm') -or $shop -eq 'supermarket') {
        $resultado.Add(@{ Tipo = 'COMPLEMENTARIO'; Rubro = $null })
    }
    if ($shop -in @('convenience', 'supermarket', 'greengrocer', 'bakery', 'deli')) {
        $resultado.Add(@{ Tipo = 'COMPETENCIA'; Rubro = 'ALMACEN' })
    }
    if ($shop -in @('kiosk', 'convenience')) {
        $resultado.Add(@{ Tipo = 'COMPETENCIA'; Rubro = 'KIOSCO' })
    }
    if ($shop -in @('stationery', 'books', 'copyshop')) {
        $resultado.Add(@{ Tipo = 'COMPETENCIA'; Rubro = 'PAPELERIA' })
    }
    return $resultado
}

$resultados = [System.Collections.Generic.List[object]]::new()
foreach ($zona in $zonas) {
    Write-Host "Consultando $($zona.Nombre)..."
    $consulta = @"
[out:json][timeout:90];
(
  nwr(around:500,$($zona.Lat),$($zona.Lon))[amenity~"^(school|kindergarten|college|university|hospital|clinic|doctors|pharmacy|bank|atm)$"];
  nwr(around:500,$($zona.Lat),$($zona.Lon))[office];
  nwr(around:500,$($zona.Lat),$($zona.Lon))[public_transport];
  nwr(around:500,$($zona.Lat),$($zona.Lon))[highway=bus_stop];
  nwr(around:500,$($zona.Lat),$($zona.Lon))[railway~"^(station|halt|subway_entrance|tram_stop)$"];
  nwr(around:500,$($zona.Lat),$($zona.Lon))[leisure~"^(park|garden|playground)$"];
  nwr(around:500,$($zona.Lat),$($zona.Lon))[shop~"^(convenience|supermarket|greengrocer|bakery|deli|kiosk|stationery|books|copyshop)$"];
);
out center tags;
"@
    $json = & curl.exe -sS -G --max-time 120 -A 'EnsamblaApp/1.0' --data-urlencode "data=$consulta" $Endpoint
    if ($LASTEXITCODE -ne 0) { throw "No se pudo consultar $($zona.Nombre)" }
    $respuesta = $json | ConvertFrom-Json
    if ($null -eq $respuesta.elements) { throw "La fuente no devolvió elementos para $($zona.Nombre)" }

    $puntos = [System.Collections.Generic.List[object]]::new()
    foreach ($elemento in $respuesta.elements) {
        $coordenadas = Coordenadas $elemento
        if ($null -eq $coordenadas) { continue }
        $nombre = if ($elemento.tags.name) { [string]$elemento.tags.name } else { "Lugar sin nombre" }
        foreach ($clasificacion in (Clasificaciones $elemento.tags)) {
            $puntos.Add(@{
                Clave = "$($elemento.type):$($elemento.id)"
                Tipo = $clasificacion.Tipo
                Rubro = $clasificacion.Rubro
                Nombre = $nombre
                Lat = $coordenadas.Lat
                Lon = $coordenadas.Lon
            })
        }
    }
    $duplicados = $puntos | Group-Object { "$($_.Clave)|$($_.Tipo)|$($_.Rubro)" } | Where-Object Count -gt 1
    if ($duplicados) { throw "La fuente devolvió puntos duplicados para $($zona.Nombre)" }
    if ($puntos | Where-Object { $_.Lat -lt -90 -or $_.Lat -gt 90 -or $_.Lon -lt -180 -or $_.Lon -gt 180 }) {
        throw "La fuente devolvió coordenadas inválidas para $($zona.Nombre)"
    }
    $resultados.Add(@{ Zona = $zona; Puntos = @($puntos) })
}

$fechaSql = $Fecha.ToString('yyyy-MM-dd')
$lineas = [System.Collections.Generic.List[string]]::new()
$lineas.Add('-- Snapshot de OpenStreetMap generado por scripts/importar-datos-zona.ps1.')
$lineas.Add('-- Los datos se conservan en PostgreSQL para que la navegación no dependa de servicios externos.')
$lineas.Add('-- Flyway ejecuta esta migración de PostgreSQL dentro de una única transacción.')
$lineas.Add('')
$lineas.Add("UPDATE fuente_dato_zona SET consultado_en = '$fechaSql', actualizado_en = now() WHERE codigo = 'OSM';")
$lineas.Add('')

foreach ($resultado in $resultados) {
    $zona = $resultado.Zona
    $lineas.Add("INSERT INTO zona (nombre, localidad, partido, provincia, latitud, longitud, radio_metros)")
    $lineas.Add("VALUES ($(Sql $zona.Nombre), $(Sql $zona.Localidad), $(Sql $zona.Partido), $(Sql $zona.Provincia), $(Numero $zona.Lat), $(Numero $zona.Lon), 700)")
    $lineas.Add('ON CONFLICT (nombre, localidad) DO UPDATE SET latitud = EXCLUDED.latitud, longitud = EXCLUDED.longitud,')
    $lineas.Add('    radio_metros = EXCLUDED.radio_metros, actualizado_en = now();')
}

$lineas.Add('')
$nombres = ($resultados | ForEach-Object { Sql $_.Zona.Nombre }) -join ', '
$lineas.Add("DELETE FROM punto_interes_zona WHERE fuente_codigo = 'OSM' AND zona_id IN")
$lineas.Add("    (SELECT id FROM zona WHERE nombre IN ($nombres));")
$lineas.Add("DELETE FROM metrica_zona WHERE fuente_codigo = 'OSM' AND zona_id IN")
$lineas.Add("    (SELECT id FROM zona WHERE nombre IN ($nombres));")
$lineas.Add('')

foreach ($resultado in $resultados) {
    $zona = $resultado.Zona
    foreach ($punto in $resultado.Puntos) {
        $rubro = Sql $punto.Rubro
        $lineas.Add('INSERT INTO punto_interes_zona')
        $lineas.Add('    (zona_id, clave_externa, tipo, rubro, nombre, latitud, longitud, fuente_codigo, observada_en)')
        $lineas.Add("SELECT id, $(Sql $punto.Clave), $(Sql $punto.Tipo), $rubro, $(Sql $punto.Nombre), $(Numero $punto.Lat), $(Numero $punto.Lon), 'OSM', '$fechaSql'")
        $lineas.Add("FROM zona WHERE nombre = $(Sql $zona.Nombre) AND localidad = $(Sql $zona.Localidad);")
    }

    $conteos = @{
        ESCUELAS = @($resultado.Puntos | Where-Object Tipo -eq 'ESCUELA').Count
        OFICINAS = @($resultado.Puntos | Where-Object Tipo -eq 'OFICINA').Count
        TRANSPORTE = @($resultado.Puntos | Where-Object Tipo -eq 'TRANSPORTE').Count
        PARQUES = @($resultado.Puntos | Where-Object Tipo -eq 'PARQUE').Count
        SALUD = @($resultado.Puntos | Where-Object Tipo -eq 'SALUD').Count
        COMPLEMENTARIOS = @($resultado.Puntos | Where-Object Tipo -eq 'COMPLEMENTARIO').Count
    }
    $actividad = [math]::Min(100, $conteos.TRANSPORTE * 4 + $conteos.OFICINAS * 3 +
        $conteos.COMPLEMENTARIOS * 2 + $conteos.ESCUELAS)
    $metricas = @(
        @{ Tipo = 'ACTIVIDAD_ACCESO'; Rubro = $null; Valor = $actividad; Unidad = 'indice_0_100' },
        @{ Tipo = 'ESCUELAS'; Rubro = $null; Valor = $conteos.ESCUELAS; Unidad = 'lugares_500m' },
        @{ Tipo = 'OFICINAS'; Rubro = $null; Valor = $conteos.OFICINAS; Unidad = 'lugares_500m' },
        @{ Tipo = 'TRANSPORTE'; Rubro = $null; Valor = $conteos.TRANSPORTE; Unidad = 'lugares_500m' },
        @{ Tipo = 'PARQUES'; Rubro = $null; Valor = $conteos.PARQUES; Unidad = 'lugares_500m' },
        @{ Tipo = 'SALUD'; Rubro = $null; Valor = $conteos.SALUD; Unidad = 'lugares_500m' },
        @{ Tipo = 'COMPLEMENTARIOS'; Rubro = $null; Valor = $conteos.COMPLEMENTARIOS; Unidad = 'lugares_500m' }
    )
    foreach ($rubro in @('ALMACEN', 'PAPELERIA', 'KIOSCO')) {
        $cantidad = @($resultado.Puntos | Where-Object { $_.Tipo -eq 'COMPETENCIA' -and $_.Rubro -eq $rubro }).Count
        $metricas += @{ Tipo = 'COMPETENCIA'; Rubro = $rubro; Valor = $cantidad; Unidad = 'comercios_500m' }
    }
    foreach ($metrica in $metricas) {
        $lineas.Add('INSERT INTO metrica_zona')
        $lineas.Add('    (zona_id, tipo, rubro, valor, unidad, fuente_codigo, observada_en, confianza)')
        $lineas.Add("SELECT id, $(Sql $metrica.Tipo), $(Sql $metrica.Rubro), $(Numero $metrica.Valor), $(Sql $metrica.Unidad), 'OSM', '$fechaSql', 80")
        $lineas.Add("FROM zona WHERE nombre = $(Sql $zona.Nombre) AND localidad = $(Sql $zona.Localidad);")
    }
}

$contenido = ($lineas -join [Environment]::NewLine) + [Environment]::NewLine
$destino = [IO.Path]::GetFullPath($SalidaSql)
$directorio = Split-Path -Parent $destino
if (-not (Test-Path $directorio)) { New-Item -ItemType Directory -Path $directorio | Out-Null }
[IO.File]::WriteAllText($destino, $contenido, [Text.UTF8Encoding]::new($false))
Write-Host "Snapshot guardado en $destino"
