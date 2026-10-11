package com.hwz.favorite.controller;

import com.hwz.admin.service.AdminAccessService;
import com.hwz.common.Result;
import com.hwz.favorite.dto.FavoriteDtos.*;
import com.hwz.favorite.service.FavoriteService;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/** 收藏接口独立命名，沿用原鉴权及响应格式，不影响学习/考核接口。 */
@RestController @RequestMapping("/favorites")
public class FavoriteController {
    private final FavoriteService service;private final AdminAccessService access;
    public FavoriteController(FavoriteService service,AdminAccessService access){this.service=service;this.access=access;}
    private Long user(){return access.requireUser().getId();}
    @GetMapping public Result<List<FavoriteView>> list(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) String tag,@RequestParam(defaultValue="newest") String sort){return Result.ok(service.list(user(),keyword,categoryId,tag,sort));}
    @GetMapping("/meta") public Result<Map<String,Object>> meta(){return Result.ok(service.meta(user()));}
    @PostMapping public Result<FavoriteView> add(@RequestBody SaveRequest req){return Result.ok(service.add(user(),req.getItemPk()));}
    @PutMapping("/{itemPk}") public Result<FavoriteView> update(@PathVariable Long itemPk,@RequestBody SaveRequest req){return Result.ok(service.update(user(),itemPk,req));}
    @DeleteMapping("/{itemPk}") public Result<Void> remove(@PathVariable Long itemPk){service.remove(user(),itemPk);return Result.ok(null);}
    @PostMapping("/import") public Result<Map<String,Integer>> importLegacy(@RequestBody ImportRequest req){return Result.ok(service.importLegacy(user(),req));}
    @PostMapping("/categories") public Result<List<Label>> addCategory(@RequestBody NameRequest req){return Result.ok(service.saveCategory(user(),null,req.getName()));}
    @PutMapping("/categories/{id}") public Result<List<Label>> renameCategory(@PathVariable Long id,@RequestBody NameRequest req){return Result.ok(service.saveCategory(user(),id,req.getName()));}
    @DeleteMapping("/categories/{id}") public Result<Void> deleteCategory(@PathVariable Long id){service.deleteCategory(user(),id);return Result.ok(null);}
}
