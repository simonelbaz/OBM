<SCRIPT language="php">
///////////////////////////////////////////////////////////////////////////////
// OBM - File : user_index.php                                               //
//     - Desc : User Index File                                              //
// 2000-01-13 Florent Goalabre                                               //
///////////////////////////////////////////////////////////////////////////////
// $Id$ //
///////////////////////////////////////////////////////////////////////////////
// Actions :
// - index (default) -- search fields  -- show the user search form
// - search          -- search fields  -- show the result set of search
// - new             --                -- show the new user form
// - detailconsult   -- $param_user    -- show the user detail
// - detailupdate    -- $param_user    -- show the user detail form
// - insert          -- form fields    -- insert the user
// - reset           -- $param_user    -- reset user preferences
// - update          -- form fields    -- update the user
// - check_delete    -- $param_user    -- check links before delete
// - delete          -- $param_user    -- delete the user
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
// Session, Auth, Perms Management                                           //
///////////////////////////////////////////////////////////////////////////////
$path = "..";
$section = "USERS";
$menu = "USER";
$obminclude = getenv("OBM_INCLUDE_VAR");
if ($obminclude == "") $obminclude = "obminclude";
require("$obminclude/phplib/obmlib.inc");
include("$obminclude/global.inc");
page_open(array("sess" => "OBM_Session", "auth" => "OBM_Challenge_Auth", "perm" => "OBM_Perm"));
include("$obminclude/global_pref.inc");

// the user MUST be "admin" to access this section

require("user_display.inc");
require("user_query.inc");

//There is no page_close(). yes, at the end
if ($action == "") $action = "index";
$obm_user = get_param_user();  // $user is used by phplib
get_user_action();
$perm->check();

// updating the user bookmark : 
if ( ($param_user == $last_user) && (strcmp($action,"delete")==0) ) {
  $last_user = $last_user_default;
} else if ( ($param_user > 0) && ($last_user != $param_user) ) {
  $last_user = $param_user;
  run_query_set_user_pref($uid, "last_user", $param_user);
  $last_user_name = run_query_global_user_name($last_user);
}

///////////////////////////////////////////////////////////////////////////////
// Main Program                                                              //
///////////////////////////////////////////////////////////////////////////////

if (! $obm_user["popup"]) {
  $display["header"] = generate_menu($menu,$section);
}

///////////////////////////////////////////////////////////////////////////////
// External calls (main menu not displayed)                                  //
///////////////////////////////////////////////////////////////////////////////
if ($action == "ext_get_ids") {
  $display["search"] = html_user_search_form($obm_user);
  if ($set_display == "yes") {
    $display["result"] = dis_user_search_list($obm_user);
  } else {
    $display["msg"] .= display_info_msg($l_no_display);
  }
}
elseif ($action == "ext_get_users") {
  $display["search"] = html_get_user_search_form($obm_user);
  if ($set_display == "yes") {
  include("user_js.inc");
    $display["result"] = dis_get_user_search_list($obm_user);
  } else {
    $display["msg"] .= display_info_msg($l_no_display);
  }
}

elseif ($action == "index" || $action == "") {
///////////////////////////////////////////////////////////////////////////////
  $display["search"] = html_user_search_form($obm_user);
  if ($set_display == "yes") {
    $display["result"] = dis_user_search_list($obm_user);
  } else {
    $display["msg"] .= display_info_msg($l_no_display);
  }

} elseif ($action == "search")  {
///////////////////////////////////////////////////////////////////////////////
  $display["search"] = html_user_search_form($obm_user);
  $display["result"] = dis_user_search_list($obm_user);

}elseif ($action == "getsearch")  {
///////////////////////////////////////////////////////////////////////////////
  include("user_js.inc");
  $display["search"] = html_get_user_search_form($obm_user);
  $display["result"] = dis_get_user_search_list($obm_user);

} elseif ($action == "new")  {
///////////////////////////////////////////////////////////////////////////////
  include("user_js.inc");
  $display["detail"] = html_user_form(1,"",$obm_user);

} elseif ($action == "detailconsult")  {
///////////////////////////////////////////////////////////////////////////////
  $obm_q = run_query_detail($obm_user["id"]);
  if ($obm_q->num_rows() == 1) {
    $display["detail"] = html_user_consult($obm_q);
  } else {
    $display["msg"] .= display_err_msg($l_query_error . " - " . $query . " !");
  }

} elseif ($action == "detailupdate")  {
///////////////////////////////////////////////////////////////////////////////
  $obm_q = run_query_detail($obm_user["id"]);
  if ($obm_q->num_rows() == 1) {
    include("user_js.inc");
    $display["detailInfo"] = display_record_info($obm_q->f("userobm_usercreate"),$obm_q->f("userobm_userupdate"),$obm_q->f("timecreate"),$obm_q->f("timeupdate")); 
    $display["detail"] = html_user_form(1, $obm_q, $obm_user);
  } else {
    $display["msg"] .= display_err_msg($l_query_error . " - " . $query . " !");
  }

} elseif ($action == "insert")  {
///////////////////////////////////////////////////////////////////////////////
  if (check_data_form("", $obm_user)) {

    // If the context (same user) was confirmed ok, we proceed
    if ($hd_confirm == $c_yes) {
      $retour = run_query_insert($obm_user);
      if ($retour) {
        $display["msg"] .= display_ok_msg($l_insert_ok);
        // insertion of his default preferences : 
        $user_id = run_query_id_user($obm_user["login"], $obm_user["passwd"]);
        run_query_default_preferences_insert($user_id);
      } else {
      $display["msg"] .= display_err_msg($l_insert_error);
      }
      $display["search"] = html_user_search_form($obm_user);

    // If it is the first try, we warn the user if some user seem similar
    } else {
      $obm_q = check_user_context("", $obm_user);
      if ($obm_q->num_rows() > 0) {
        $display["detail"] = dis_user_warn_insert("", $obm_q, $obm_user);
      } else {
        $retour = run_query_insert($obm_user);
        if ($retour) {
          // insertion of his default preferences : 
          $user_id = run_query_id_user($obm_user["login"], $obm_user["passwd"]);
          run_query_default_preferences_insert($user_id);
          $display["msg"] .= display_ok_msg($l_insert_ok);
        } else {
          $display["msg"] .= display_err_msg($l_insert_error);
        }
        $display["search"] = html_user_search_form($obm_user);
      }
    }

  // Form data are not valid
  } else {
    $display["msg"] .= display_warn_msg($l_invalid_data . " : " . $err_msg);
    $display["detail"] = html_user_form(0, "", $obm_user);
  }

} elseif ($action == "reset")  {
///////////////////////////////////////////////////////////////////////////////
  run_query_default_preferences_insert($obm_user["id"]);
  session_load_user_prefs();
  $display["msg"] .= display_ok_msg($l_reset_ok);
  $obm_q = run_query_detail($obm_user["id"]);
  if ($obm_q->num_rows() == 1) {
    $display["detail"] = html_user_consult($obm_q);
  } else {
    $display["msg"] .= display_err_msg($l_query_error . " - " . $query . " !");
  }

} elseif ($action == "update")  {
///////////////////////////////////////////////////////////////////////////////
  if (check_data_form($obm_user["id"], $obm_user)) {
    $retour = run_query_update($obm_user["id"], $obm_user);
    if ($retour) {
      $display["msg"] .= display_ok_msg($l_update_ok);
    } else {
      $display["msg"] .= display_err_msg($l_update_error);
    }
    $display["search"] = html_user_search_form($obm_user);
  } else {
    $display["msg"] .= display_err_msg($err_msg);
  }

} elseif ($action == "check_delete")  {
///////////////////////////////////////////////////////////////////////////////
  require("user_js.inc");
  $display["detail"] = dis_check_links($obm_user["id"]);

} elseif ($action == "delete")  {
///////////////////////////////////////////////////////////////////////////////
  $retour = run_query_delete($obm_user["id"]);
  if ($retour) {
    $display["msg"] .= display_ok_msg($l_delete_ok);
  } else {
    $display["msg"] .= display_err_msg($l_delete_error);
  }
  run_query_delete_profil($obm_user["id"]);
  $display["search"] = html_user_search_form($obm_user);

} elseif ($action == "admin")  {
///////////////////////////////////////////////////////////////////////////////
  $display["detail"] = "<center>Nothing here</center><br />";

}  elseif ($action == "display") {
///////////////////////////////////////////////////////////////////////////////
  $pref_q = run_query_display_pref($auth->auth["uid"], "user", 1);
  $display["detail"] = dis_user_display_pref($pref_q);

} else if ($action == "dispref_display") {
///////////////////////////////////////////////////////////////////////////////
  run_query_display_pref_update($entity, $fieldname, $disstatus);
  $pref_q = run_query_display_pref($auth->auth["uid"], "user", 1);
  $display["detail"] = dis_user_display_pref($pref_q);

} else if ($action == "dispref_level") {
///////////////////////////////////////////////////////////////////////////////
  run_query_display_pref_level_update($entity, $new_level, $fieldorder);
  $pref_q = run_query_display_pref($auth->auth["uid"], "user", 1);
  $display["detail"] = dis_user_display_pref($pref_q);
}


///////////////////////////////////////////////////////////////////////////////
// Display
///////////////////////////////////////////////////////////////////////////////
$display["head"] = display_head($l_user);
$display["end"] = display_end();

display_page($display);


///////////////////////////////////////////////////////////////////////////////
// Stores User parameters transmited in $obm_user hash
// returns : $obm_user hash with parameters set
///////////////////////////////////////////////////////////////////////////////
function get_param_user() {
  global $cdg_param, $popup;
  global $param_user, $tf_login, $tf_passwd, $sel_perms, $tf_email;
  global $tf_lastname, $tf_firstname, $tf_phone, $cb_archive;
  global $param_ext, $ext_action, $ext_url, $ext_id, $ext_title, $ext_target;

  if (isset ($param_ext)) $obm_user["id"] = $param_ext;
  if (isset ($param_user)) $obm_user["id"] = $param_user;
  if (isset ($popup)) $obm_user["popup"] = 1;
  if (isset ($tf_login)) $obm_user["login"] = $tf_login;
  if (isset ($tf_passwd)) $obm_user["passwd"] = $tf_passwd;
  if (isset ($sel_perms)) $obm_user["perms"] = $sel_perms;
  if (isset ($tf_email)) $obm_user["email"] = $tf_email;
  if (isset ($tf_lastname)) $obm_user["lastname"] = $tf_lastname;
  if (isset ($tf_firstname)) $obm_user["firstname"] = $tf_firstname;
  if (isset ($tf_phone)) $obm_user["phone"] = $tf_phone;
  if (isset ($cb_archive)) $obm_user["archive"] = $cb_archive;

  // External param
  if (isset ($popup)) $obm_user["popup"] = $popup;
  if (isset ($ext_title)) $obm_user["ext_title"] = $ext_title;
  if (isset ($ext_action)) $obm_user["ext_action"] = $ext_action;
  if (isset ($ext_url)) $obm_user["ext_url"] = $ext_url;
  if (isset ($ext_id)) $obm_user["ext_id"] = $ext_id;
  if (isset ($ext_target)) $obm_user["ext_target"] = $ext_target;

  if (debug_level_isset($cdg_param)) {
    if ( $obm_user ) {
      while ( list( $key, $val ) = each( $obm_user ) ) {
        echo "<br />user[$key]=$val";
      }
    }
  }

  return $obm_user;
}


///////////////////////////////////////////////////////////////////////////////
// User Action 
///////////////////////////////////////////////////////////////////////////////
function get_user_action() {
  global $obm_user, $actions, $path;
  global $l_header_find,$l_header_new,$l_header_update,$l_header_delete;
  global $l_header_consult,$l_header_display,$l_header_admin,$l_header_reset;
  global $user_read, $user_write, $user_admin_read, $user_admin_write;

// Index
  $actions["USER"]["index"] = array (
    'Name'     => $l_header_find,
    'Url'      => "$path/user/user_index.php?action=index",
    'Right'    => $user_read,
    'Condition'=> array ('all') 
                                    );

// New
  $actions["USER"]["new"] = array (
    'Name'     => $l_header_new,
    'Url'      => "$path/user/user_index.php?action=new",
    'Right'    => $user_write,
    'Condition'=> array ('search','index','admin','detailconsult','reset','display') 
                                  );

// Search
  $actions["USER"]["search"] = array (
    'Url'      => "$path/user/user_index.php?action=search",
    'Right'    => $user_read,
    'Condition'=> array ('None') 
                                  );
  
// Get user id from external window (js)
  $actions["USER"]["getsearch"] = array (
    'Url'      => "$path/user/user_index.php?action=search",
    'Right'    => $user_read,
    'Condition'=> array ('None') 
                                  );
// Detail Consult
  $actions["USER"]["detailconsult"] = array (
    'Name'     => $l_header_consult,
    'Url'      => "$path/user/user_index.php?action=detailconsult&amp;param_user=".$obm_user["id"]."",
    'Right'    => $user_read,
    'Condition'=> array ('detailupdate') 
                                  );

// Detail Update
  $actions["USER"]["detailupdate"] = array (
     'Name'     => $l_header_update,
     'Url'      => "$path/user/user_index.php?action=detailupdate&amp;param_user=".$obm_user["id"]."",
     'Right'    => $user_write,
     'Condition'=> array ('detailconsult', 'reset') 
                                     	   );

// Reset
  $actions["USER"]["reset"] = array (
    'Name'     => $l_header_reset,
    'Url'      => "$path/user/user_index.php?action=reset&amp;param_user=".$obm_user["id"]."",
    'Right'    => $user_write,
    'Condition'=> array ('detailconsult') 
                                    );

// Insert
  $actions["USER"]["insert"] = array (
    'Url'      => "$path/user/user_index.php?action=insert",
    'Right'    => $user_write,
    'Condition'=> array ('None') 
                                     );

// Update
  $actions["USER"]["update"] = array (
    'Url'      => "$path/user/user_index.php?action=update",
    'Right'    => $user_write,
    'Condition'=> array ('None') 
                                     );

// Check Delete
  $actions["USER"]["check_delete"] = array (
    'Name'     => $l_header_delete,
    'Url'      => "$path/user/user_index.php?action=check_delete&amp;param_user=".$obm_user["id"]."",
    'Right'    => $user_write,
    'Condition'=> array ('detailconsult', 'detailupdate', 'reset') 
                                     	   );

// Delete
  $actions["USER"]["delete"] = array (
    'Url'      => "$path/user/user_index.php?action=delete",
    'Right'    => $user_write,
    'Condition'=> array ('None') 
                                     );

// Admin
  $actions["USER"]["admin"] = array (
    'Name'     => $l_header_admin,
    'Url'      => "$path/user/user_index.php?action=admin",
    'Right'    => $user_admin_read,
    'Condition'=> array ('all') 
                                    );

// Dispay
  $actions["USER"]["display"] = array (
    'Name'     => $l_header_display,
    'Url'      => "$path/user/user_index.php?action=display",
    'Right'    => $user_read,
    'Condition'=> array ('all') 
                                      	 );

}

</script>
