<SCRIPT language="php">
///////////////////////////////////////////////////////////////////////////////
// OBM - File : contract_index.php                                           //
//     - Desc : Contract Support Index File                                  //
// 2001-07-17 : Aliacom                                                      //
///////////////////////////////////////////////////////////////////////////////
//  $Id$
///////////////////////////////////////////////////////////////////////////////
// Actions :
// - index (default) -- search fields   -- show the Contract search form
// - search          -- search fields   -- show the result set of search
// - new             -- $param_company, -- show the new Contract form
// - detailconsult   -- $param_contract -- show the Contract detail
// - detailupdate    -- $param_contract -- show the Contract detail form
// - insert          -- form fields     -- insert the Contract 
// - update          -- form fields     -- update the Contract
// - check_delete    -- $param_contract -- check links before delete
// - delete          -- $param_contract -- delete the Contract
// - admin	     --		        -- admin index (kind)
// - type_insert     -- form fields     -- insert the type
// - type_update     -- form fields     -- update the type
// - type_checklink  -- $sel_kind       -- check if kind is used
// - type_delete     -- $sel_kind       -- delete the type
// - display         --                 -- display and set display parameters
// - dispref_display --                 -- update one field display value
// - dispref_level   --                 -- update one field display position 
// External API ---------------------------------------------------------------
// - ext_get_id      -- $title          -- select a contract (return id) 
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
// Session,Auth,Perms Management                                             //
///////////////////////////////////////////////////////////////////////////////
$path = "..";
$section = "PROD";
$menu="CONTRACT";
$obminclude = getenv("OBM_INCLUDE_VAR");
if ($obminclude == "") $obminclude = "obminclude";
require("$obminclude/phplib/obmlib.inc");
include("$obminclude/global.inc");
page_open(array("sess" => "OBM_Session", "auth" => "OBM_Challenge_Auth", "perm" => "OBM_Perm"));
$perm->check("user");
include("$obminclude/global_pref.inc");

require("contract_query.inc");
require("contract_display.inc");


// Updating the "last contract" bookmark 
if ( ($param_contract == $last_contract) && (strcmp($action,"delete")==0) ) {
  $last_contract=$last_contract_default;
} elseif  ( ($param_contract > 0) && ($last_contract != $param_contract) ) {
  $last_contract=$param_contract;
  run_query_set_user_pref($auth->auth["uid"],"last_contract",$param_contract);
  $last_contract_name = run_query_global_contract_label($last_contract);
  //$sess->register("last_contract");
}

page_close();

if ($action == "") $action = "index";
$contract = get_param_contract();
get_contract_action();
$perm->check();


///////////////////////////////////////////////////////////////////////////////
// Main Program                                                              //
///////////////////////////////////////////////////////////////////////////////
if (! $popup) {
  $display["header"] = generate_menu($menu, $section);
}

///////////////////////////////////////////////////////////////////////////////
// External calls (main menu not displayed)                                  //
///////////////////////////////////////////////////////////////////////////////
if ($action == "ext_get_id") {
  require("contract_js.inc");
  $con_q = run_query_contract();
  $display["detail"] = html_select_contract($con_q, $contract["ext_title"]);

///////////////////////////////////////////////////////////////////////////////
// Normal calls
///////////////////////////////////////////////////////////////////////////////
} elseif ($action == "index" || $action == "") {
///////////////////////////////////////////////////////////////////////////////
  require("contract_js.inc");
  $usr_q = run_query_userobm();
  $display["search"] = html_contract_search_form($contract, $usr_q, run_query_contracttype());
  if ($set_display == "yes") {
    $display["result"] = dis_contract_search_list($contract);
  } else {
    $display["msg"] .= display_info_msg($l_no_display);
  }
  
} elseif ($action == "search")  {
///////////////////////////////////////////////////////////////////////////////
  require("contract_js.inc");
  $usr_q = run_query_userobm();
  $display["search"] = html_contract_search_form($contract, $usr_q, run_query_contracttype());
  $display["result"] = dis_contract_search_list($contract);
  
} elseif ($action == "new")  {
///////////////////////////////////////////////////////////////////////////////
  require("contract_js.inc");
  if ($param_deal != "")
    $display["msg"] .= display_ok_msg(stripslashes($ok_message)."<br />".$l_add_contract_deal);
  $display["detail"] = html_contract_form($action,new DB_OBM,run_query_contracttype(),run_query_userobm_active(),run_query_company_info($param_company),run_query_contact_contract($param_company), $contract);

} elseif ($action == "detailconsult")  {
///////////////////////////////////////////////////////////////////////////////
  if ($param_contract > 0) {
    $contract_q = run_query_detail($param_contract);
    $display["detailInfo"] = display_record_info($contract_q->f("contract_usercreate"),$contract_q->f("contract_userupdate"),$contract_q->f("timecreate"),$contract_q->f("timeupdate"));
    $display["detail"] = html_contract_consult($contract_q);
  }
  
} elseif ($action == "detailupdate")  {
//////////////////////////////////////////////////////////////////////////////
  if ($param_contract > 0) {
    $contract_q = run_query_detail($param_contract);
    require("contract_js.inc");
    $display["detailInfo"] = display_record_info($contract_q->f("contract_usercreate"),$contract_q->f("contract_userupdate"),$contract_q->f("timecreate"),$contract_q->f("timeupdate"));
    $users = array($contract_q->f("contract_marketmanager_id"), $contract_q->f("contract_techmanager_id"));
    $usr_q = run_query_userobm_active($users);
    $company = $contract_q->f("contract_company_id");
    $display["detail"] = html_contract_form($action,$contract_q,run_query_contracttype(), $usr_q, run_query_company_info($company), run_query_contact_contract($company), $contract);
  }

} elseif ($action == "insert")  {
///////////////////////////////////////////////////////////////////////////////
  if (check_contract_form("", $contract)) {
    $retour = run_query_insert($contract);
    if ($retour) {
      $display["msg"] .= display_ok_msg($l_insert_ok);
    } else {
      $display["msg"] .= display_err_msg($l_insert_error);
    }
    require("contract_js.inc");
    $usr_q = run_query_userobm();
    $display["search"] = html_contract_search_form($contract, $usr_q, run_query_contracttype());
  } else {
    $display["msg"] .= display_err_msg($err_msg);
    $users = array($contract["market"], $contract["tech"]);
    $usr_q = run_query_userobm_active($users);
    $display["detail"] = html_contract_form($action,new DB_OBM,run_query_contracttype(), $usr_q, run_query_company_info($param_company),run_query_contact_contract($param_company), $contract);
 }

} elseif ($action == "update")  {
///////////////////////////////////////////////////////////////////////////////
  if (check_contract_form("", $contract)) {  
    $ret = run_query_update($contract);         
    if ($ret) {
      $display["msg"] .= display_ok_msg($l_update_ok);
    } else {
     $display["msg"] .= display_err_msg($l_update_error);
    }
    $usr_q = run_query_userobm();
    $display["search"] = html_contract_search_form($contract, $usr_q, run_query_contracttype());
  } else {
    require("contract_js.inc");
    $display["msg"] .= display_err_msg($l_invalid_data . " : " . $err_msg);
    $users = array($contract["market"], $contract["tech"]);
    $usr_q = run_query_userobm_active($users);
    $display["detail"] = html_contract_form($action,new DB_OBM,run_query_contracttype(), $usr_q, run_query_company_info($param_company),run_query_contact_contract($param_company), $contract);
  }

} elseif ($action == "check_delete")  {
///////////////////////////////////////////////////////////////////////////////
  require("contract_js.inc");
  $display["detail"] = dis_check_links($param_contract);

} elseif ($action == "delete")  {
///////////////////////////////////////////////////////////////////////////////
  $ret = run_query_delete($param_contract);
  if ($ret) {
    $display["msg"] .= display_ok_msg($l_delete_ok);
  } else {
    $display["msg"] .= display_err_msg($l_delete_error);
  }
  $usr_q = run_query_userobm();
  $display["search"] = html_contract_search_form($contract, $usr_q, run_query_contracttype());

} elseif ($action == "display") {
///////////////////////////////////////////////////////////////////////////////
  $pref_q = run_query_display_pref($auth->auth["uid"], "contract", 1);
  $display["detail"] = dis_contract_display_pref($pref_q);
  
} elseif ($action == "dispref_display") {
///////////////////////////////////////////////////////////////////////////////
  run_query_display_pref_update($entity, $fieldname, $disstatus);
  $pref_q = run_query_display_pref($auth->auth["uid"], "contract", 1);
  $display["detail"] = dis_contract_display_pref($pref_q);
  
} elseif ($action == "dispref_level") {
///////////////////////////////////////////////////////////////////////////////
  run_query_display_pref_level_update($entity, $new_level, $fieldorder);
  $pref_q = run_query_display_pref($auth->auth["uid"], "contract", 1);
  $display["detail"] = dis_contract_display_pref($pref_q);
  
} elseif ($action == "admin")  {
//////////////////////////////////////////////////////////////////////////////
  require("contract_js.inc");
  $display["detail"] = html_contract_admin_form(run_query_contracttype());
  
} elseif ($action == "type_insert")  {
///////////////////////////////////////////////////////////////////////////////
  $retour = run_query_type_insert($contract);
  if ($retour) {
    $display["msg"] .= display_ok_msg($l_type_insert_ok);
  } else {
    $display["msg"] .= display_err_msg($l_type_insert_error);
  }
  require("contract_js.inc");
  $display["detail"] = html_contract_admin_form(run_query_contracttype());
    
} elseif ($action == "type_update")  {
///////////////////////////////////////////////////////////////////////////////
  $retour = run_query_type_update($contract);
  if ($retour) {
    $display["msg"] .= display_ok_msg($l_type_update_ok);
  } else {
    $display["msg"] .= display_err_msg($l_type_update_error);
  }
  require("contract_js.inc");
  $display["detail"] = html_contract_admin_form(run_query_contracttype());

} elseif ($action == "type_checklink")  {
///////////////////////////////////////////////////////////////////////////////
  $display["detail"] = dis_type_links($contract["type"]);
  require("contract_js.inc");
  
} elseif ($action == "type_delete")  {
///////////////////////////////////////////////////////////////////////////////
  $retour = run_query_type_delete($contract["type"]);
  if ($retour) {
    $display["msg"] .= display_ok_msg($l_type_delete_ok);
  } else {
    $display["msg"] .= display_err_msg($l_type_delete_error);
  }
  require("contract_js.inc");
  $display["detail"] = html_contract_admin_form(run_query_contracttype());
}


///////////////////////////////////////////////////////////////////////////////
// Display
///////////////////////////////////////////////////////////////////////////////
$display["head"] = display_head($l_contract);
$display["end"] = display_end();

display_page($display);


///////////////////////////////////////////////////////////////////////////////
// Stores Contract parameters transmited in $contract hash
// returns : $contract hash with parameters set
///////////////////////////////////////////////////////////////////////////////
function get_param_contract() {
  global $tf_label,$tf_company_name,$sel_type, $tf_type;
  global $tf_dateafter,$tf_datebefore,$sel_manager,$cb_arc,$param_company;
  global $param_contract,$tf_num,$sel_market, $sel_tech;
  global $ta_clause,$ta_com,$sel_con1, $sel_con2,$tf_datebegin,$tf_dateexp;
  global $hd_usercreate,$hd_timeupdate,$param_deal,$deal_label,$deal_new_id;
  global $hd_company_ad1, $hd_company_zip, $hd_company_town;
  global $ext_title;
  global $cdg_param, $action;

  if (isset ($param_contract)) $contract["id"] = $param_contract;
  if (isset ($param_company)) $contract["company_id"] = $param_company;

  if (isset ($tf_label)) $contract["label"] = $tf_label;
  if (isset ($tf_datebegin)) $contract["datebegin"] = $tf_datebegin;
  if (isset ($tf_dateexp)) $contract["dateexp"] = $tf_dateexp;
  if (isset ($tf_num)) $contract["number"] = $tf_num;

  if (isset ($sel_market)) $contract["market"] = $sel_market;
  if (isset ($sel_tech)) $contract["tech"] = $sel_tech;
  if (isset ($sel_con1)) $contract["contact1"] = $sel_con1;
  if (isset ($sel_con2)) $contract["contact2"] = $sel_con2;
  if (isset ($sel_type)) $contract["type"] = $sel_type;

  if (isset ($cb_arc)) $contract["arc"] = $cb_arc;

  if (isset ($ta_clause)) $contract["clause"] = $ta_clause;  
  if (isset ($ta_com)) $contract["comment"] = $ta_com;  

  if (isset ($hd_usercreate)) $contract["usercreate"] = $hd_usercreate;
  if (isset ($hd_timeupdate)) $contract["timeupdate"] = $hd_timeupdate;

  // Search fields
  if (isset ($tf_dateafter)) $contract["dateafter"] = $tf_dateafter;
  if (isset ($tf_datebefore)) $contract["datebefore"] = $tf_datebefore;
  if (isset ($sel_manager)) $contract["manager"] = $sel_manager;
  if (isset ($tf_company_name)) $contract["company_name"] = $tf_company_name;

  // Company infos (with company_name)
  if (isset ($hd_company_ad1)) $contract["company_ad1"] = $hd_company_ad1;
  if (isset ($hd_company_zip)) $contract["company_zip"] = $hd_company_zip;
  if (isset ($hd_company_town)) $contract["company_town"] = $hd_company_town;

  // Deal infos
  if (isset ($param_deal)) $contract["deal_id"] = $param_deal;
  if (isset ($deal_new_id)) $contract["deal_new_id"] = $deal_new_id;
  if (isset ($deal_label)) $contract["deal_label"] = $deal_label;

  if (isset ($tf_type)) $contract["type_label"] = $tf_type;

  if (isset ($ext_title)) $contract["ext_title"] = stripslashes(urldecode($ext_title));

  if (debug_level_isset($cdg_param)) {
    echo "<br>action=$action";
    if ( $contract ) {
      while ( list( $key, $val ) = each( $contract ) ) {
        echo "<br>contract[$key]=$val";
      }
    }
  }

  return $contract;
}


//////////////////////////////////////////////////////////////////////////////
// Contract actions
//////////////////////////////////////////////////////////////////////////////
function get_contract_action() {
  global $contract, $actions, $path, $l_select_company;
  global $l_header_find,$l_header_new,$l_header_update,$l_header_delete;
  global $l_header_consult, $l_header_display, $l_header_admin;
  global $contract_read, $contract_write, $contract_admin_read, $contract_admin_write;

// Ext Get Id
  $actions["CONTRACT"]["ext_get_id"] = array (
    'Url'      => "$path/contract/contract_index.php?action=ext_get_id",
    'Right'    => $contract_read,
    'Condition'=> array ('None') 
                                    	);

// Index
  $actions["CONTRACT"]["index"] = array (
    'Name'     => $l_header_find,
    'Url'      => "$path/contract/contract_index.php?action=index",
    'Right'    => $contract_read,
    'Condition'=> array ('all') 
                                    	);

// Search
  $actions["CONTRACT"]["search"] = array (
    'Url'      => "$path/contract/contract_index.php?action=search",
    'Right'    => $contract_read,
    'Condition'=> array ('None') 
                                    	);

// New
  $actions["CONTRACT"]["new"] = array (
    'Name'     => $l_header_new,
    'Url'      => "$path/company/company_index.php?action=ext_get_id_url&amp;popup=1&amp;ext_title=".urlencode($l_select_company)."&amp;ext_url=".urlencode("$path/contract/contract_index.php?action=new&amp;param_company=")."",
    'Right'    => $contract_write,
    'Popup'    => 1,
    'Condition'=> array ('','search','index','detailconsult','admin','display')
                                      );

// Insert
  $actions["CONTRACT"]["insert"] = array (
    'Url'      => "$path/contract/contract_index.php?action=insert",
    'Right'    => $contract_write,
    'Condition'=> array ('None') 
                                    	);

// Detail Consult
  $actions["CONTRACT"]["detailconsult"] = array (
    'Name'     => $l_header_consult,
    'Url'      => "$path/contract/contract_index.php?action=detailconsult&amp;param_contract=".$contract["id"]."",
    'Right'    => $contract_read, 
    'Condition'=> array ('detailupdate') 
                                    	);

// Detail Update
  $actions["CONTRACT"]["detailupdate"] = array (
    'Name'     => $l_header_update,
    'Url'      => "$path/contract/contract_index.php?action=detailupdate&amp;param_contract=".$contract["id"]."",
    'Right'    => $contract_write,
    'Condition'=> array ('detailconsult') 
                                     	 );

// Update
  $actions["CONTRACT"]["update"] = array (
    'Url'      => "$path/contract/contract_index.php?action=update",
    'Right'    => $contract_write,
    'Condition'=> array ('None') 
                                    	);

// Delete
  $actions["CONTRACT"]["check_delete"] = array (
    'Name'     => $l_header_delete,
    'Url'      => "$path/contract/contract_index.php?action=check_delete&amp;param_contract=".$contract["id"]."",
    'Right'    => $contract_write,
    'Condition'=> array ('detailconsult') 
                                     	 );
// Admin
  $actions["CONTRACT"]["admin"] = array (
    'Name'     => $l_header_admin,
    'Url'      => "$path/contract/contract_index.php?action=admin",
    'Right'    => $contract_admin_read, 
    'Condition'=> array ('all') 
                                      	);

// Admin Type Insert
  $actions["CONTRACT"]["type_insert"] = array (
    'Url'      => "$path/contract/contract_index.php?action=type_insert",
    'Right'    => $contract_admin_write,
    'Condition'=> array ('None') 
                                    	);

// Admin Type Insert
  $actions["CONTRACT"]["type_delete"] = array (
    'Url'      => "$path/contract/contract_index.php?action=type_delete",
    'Right'    => $contract_admin_write,
    'Condition'=> array ('None') 
                                    	);

// Admin Type Update
  $actions["CONTRACT"]["type_update"] = array (
    'Url'      => "$path/contract/contract_index.php?action=type_update",
    'Right'    => $contract_admin_write,
    'Condition'=> array ('None') 
                                    	);

// Display
  $actions["CONTRACT"]["display"] = array (
    'Name'     => $l_header_display,
    'Url'      => "$path/contract/contract_index.php?action=display",
    'Right'    => $contract_read,
    'Condition'=> array ('all') 
                                      	  );

// Display Préférence
  $actions["CONTRACT"]["dispref_display"] = array (
    'Url'      => "$path/contract/contract_index.php?action=dispref_display",
    'Right'    => $contract_read,
    'Condition'=> array ('None') 
                                        	  );

// Display Level
  $actions["CONTRACT"]["dispref_level"] = array (
    'Url'      => "$path/contract/contract_index.php?action=dispref_level",
    'Right'    => $contract_read,
    'Condition'=> array ('None') 
                                    	       );

}
///////////////////////////////////////////////////////////////////////////////
// Display end of page                                                       //
///////////////////////////////////////////////////////////////////////////////
display_end();


</SCRIPT>
