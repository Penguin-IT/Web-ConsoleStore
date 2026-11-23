package com.example.consolestore.Controller;

import com.example.consolestore.DTO.AddressRequest;
import com.example.consolestore.DTO.AddressView;
import com.example.consolestore.Service.AddressBookService;
import com.example.consolestore.Service.AddressRejectedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/checkout/addresses")
public class AddressBookController {

    private final AddressBookService book;

    public AddressBookController(AddressBookService book) {
        this.book = book;
    }

    @GetMapping
    public List<AddressView> list(Principal principal) {
        return book.list(principal.getName());
    }

    @PostMapping
    public AddressView create(@ModelAttribute AddressRequest req, Principal principal) {
        return book.save(principal.getName(), null, req);
    }

    @PostMapping("/{id}")
    public AddressView update(@PathVariable Long id,
                              @ModelAttribute AddressRequest req,
                              Principal principal) {
        return book.save(principal.getName(), id, req);
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<Void> makeDefault(@PathVariable Long id, Principal principal) {
        book.makeDefault(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/delete")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        book.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AddressRejectedException.class)
    public ResponseEntity<Map<String, Object>> rejected(AddressRejectedException e) {
        return ResponseEntity.status(422).body(Map.of("errors", e.errors()));
    }
}