package com.clevertricks;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.ui.Model;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class FileUploadController {
    private final StorageService storageService;

    @Autowired
    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    private String ownerOf(OidcUser user) {
        return user.getSubject();
    }

    @GetMapping("/files")
    public String listUploadedFiles(Model model, @AuthenticationPrincipal OidcUser oidcUser) {
        String owner = ownerOf(oidcUser);
        model.addAttribute("files", storageService.loadAll(owner).collect(Collectors.toList()));
        model.addAttribute("username", oidcUser.getPreferredUsername());
        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename,
            @AuthenticationPrincipal OidcUser oidcUser) {
        Resource file = storageService.loadAsResource(filename, ownerOf(oidcUser));

        if (file == null)
            return ResponseEntity.notFound().build();

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes, @AuthenticationPrincipal OidcUser oidcUser) {
        try {
            storageService.store(file, ownerOf(oidcUser));
        } catch (StorageFileAlreadyExistsException e) {
            redirectAttributes.addFlashAttribute("message",
                    "File with same name already exists, you need to delete it first to ");
            return "redirect:/files";
        } catch (StorageFileEmptyException e) {
            redirectAttributes.addFlashAttribute("message", "Cannot upload empty file");
            return "redirect:/files";
        }
        redirectAttributes.addFlashAttribute("message", "You successfully uploaded " + file.getOriginalFilename());
        return "redirect:/files";
    }

    @PostMapping("/files/delete")
    public String deleteFiles(@RequestParam List<String> files, RedirectAttributes redirectAttributes,
            @AuthenticationPrincipal OidcUser oidcUser) {
        String owner = ownerOf(oidcUser);
        files.forEach(f -> storageService.delete(f, owner));
        redirectAttributes.addFlashAttribute("message", "Deleted " + files.size() + " file(s)");
        return "redirect:/files";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
