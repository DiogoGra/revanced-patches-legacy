# Publicar parches para Morphe Manager

Esta guia describe el flujo obligatorio para publicar una version sin que
Morphe Manager muestre `0` parches o `N/A` como version.

## Regla critica

El MPP para Android se genera con:

```powershell
.\gradlew.bat :patches:buildAndroid --no-daemon
```

No se debe publicar el archivo generado solamente por `:patches:assemble`.
Ese MPP contiene las clases JVM y Morphe CLI puede leerlo en una PC, pero no
incluye `classes.dex`. Morphe Manager en Android no puede cargar sus parches.

## Flujo de publicacion

1. Incrementar `version` en `gradle.properties`. No reutilizar el nombre de una
   version publicada.
2. Compilar con `:patches:buildAndroid`.
3. Verificar que el MPP incluya `classes.dex` y no incluya Kotlin runtime.
4. Commitear y subir el codigo a la rama `dev`.
5. Crear una GitHub pre-release cuyo tag, titulo y asset tengan exactamente la
   misma version.
6. Subir solamente el MPP principal, por ejemplo
   `patches-1.3.0-dev.6.mpp`.
7. Despues de publicar la release, actualizar en `patches-bundle.json`:
   `created_at`, `description`, `download_url` y `version`. `created_at` debe
   ser la fecha de creacion del asset MPP, no la fecha de la release. Morphe
   usa este valor para invalidar su cache.
8. Commitear y subir `patches-bundle.json` a `dev`.
9. Descargar el asset publicado, comparar su SHA-256 con el MPP local y pedirle
   a Morphe CLI que enumere los parches desde el archivo descargado.

## Verificacion obligatoria del MPP

```powershell
$mpp = ".\patches\build\libs\patches-VERSION.mpp"
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [IO.Compression.ZipFile]::OpenRead((Resolve-Path $mpp))
try {
    if (-not $zip.GetEntry("classes.dex")) {
        throw "MPP invalido para Android: falta classes.dex"
    }

    $kotlinEntries = $zip.Entries.FullName | Where-Object {
        $_ -like "kotlin/*" -or $_ -like "kotlinx/*"
    }
    if ($kotlinEntries) {
        throw "MPP invalido: contiene Kotlin runtime"
    }
} finally {
    $zip.Dispose()
}
```

## Verificacion del asset remoto

```powershell
$local = ".\patches\build\libs\patches-VERSION.mpp"
$remote = ".\patches-VERSION-remote.mpp"
$url = "https://github.com/DiogoGra/revanced-patches-legacy/releases/download/VERSION/patches-VERSION.mpp"

Invoke-WebRequest -Uri $url -OutFile $remote
if ((Get-FileHash $local).Hash -ne (Get-FileHash $remote).Hash) {
    throw "El asset publicado no coincide con el MPP local"
}
```

Luego ejecutar `list-patches` de Morphe CLI usando el MPP descargado. La
validacion falla si devuelve cero parches o no menciona YouTube `19.16.39`.

## Formato de release

```markdown
### Bug Fixes

- **YouTube - Patch name**: Short description. (https://github.com/DiogoGra/revanced-patches-legacy/commit/FULL_COMMIT_SHA)
```

El enlace del commit debe usar el SHA completo y permanecer en una sola linea.
