mod lib;
use std::env;
use std::ffi::{c_char, CStr, CString};
use anyhow::{anyhow, Error};

pub fn main() {
    let args: Vec<String> = env::args().collect();
    let result = handle(args);
    match result {
        Ok(out) => {println!("{}", out);},
        Err(e) => {println!("{{\"ok\": false, \"msg\": \"{e}\"}}");}
    }
}

pub fn handle(args: Vec<String>) -> Result<String, Error> {
    let fn_name = args.get(1).unwrap_or(&"".to_string()).to_owned();
    let fn_input = convert_input(args.get(2).unwrap_or(&"".to_string()).to_owned())?;
    match fn_name.as_str() {
        "radHome" => {
            let res = lib::radHome(fn_input);
            let result = unsafe { CStr::from_ptr(res) }.to_str()?;
            Ok(result.to_string())
        },
        "changeIssueTitleDescription" => lib::handle_change_issue_title_description(fn_input),
        "editIssueComment" => lib::handle_edit_issue_comment(fn_input),
        "getEmbeds" => lib::handle_get_embeds(fn_input),
        "getAlias" => lib::handle_get_alias(fn_input),
        "createPatchComment" => lib::handle_create_patch_comment(fn_input),
        "editPatchComment" => lib::handle_edit_patch_comment(fn_input),
        "deletePatchComment" => lib::handle_delete_patch_comment(fn_input),
        "patchCommentReact" => lib::handle_patch_comment_react(fn_input),
        "issueCommentReact" => lib::handle_issue_comment_react(fn_input),
        _ => Err(anyhow!("no such method: {fn_name}")),
    }
}

pub fn convert_input(input: String) -> Result<*const c_char, Error> {
    let input_cstr = CString::new(input.as_bytes())?;
    Ok(input_cstr.as_ptr())
}