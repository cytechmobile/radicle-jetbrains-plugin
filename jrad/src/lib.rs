use radicle::cob::ObjectId;
use radicle::git::raw::IntoCString;
use radicle::prelude::RepoId;
use radicle::storage::ReadStorage;
use radicle::Profile;
use serde::{Deserialize, Serialize};
use std::ffi::c_char;
use std::str::FromStr;

pub fn read_input(input: *const c_char) -> Result<String, anyhow::Error> {
    let input_str = unsafe { std::ffi::CStr::from_ptr(input) };
    let input_string = input_str.to_str()?;
    Ok(String::from(input_string))
}

pub fn construct_result(input: String) -> *const c_char {
    input.to_string().into_c_string().unwrap().into_raw()
}

#[no_mangle]
pub extern "system" fn radHome(inp: *const c_char) -> *const c_char {
    // let input_res = read_input(inp);
    let p = Profile::load().unwrap();
    let result = p.home.path().to_str().unwrap();
    construct_result(String::from(result))
}

#[derive(Serialize,Deserialize,Clone,Debug)]
struct ChangeIssueTitle {
    repo_id: String,
    issue_id: String,
    title: String,
    description: String,
}

#[no_mangle]
pub extern "system" fn changeIssueTitle(inp: *const c_char) -> *const c_char {
    let result = handle_change_issue_title(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => {
            construct_result(format!("{{\"ok\": false, \"msg\": \"{e}\"}}"))
        }
    }
}

pub fn handle_change_issue_title(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let cit: ChangeIssueTitle = serde_json::from_str(&input)?;
    let p = Profile::load()?;
    let rid = RepoId::from_urn(&cit.repo_id)?;
    let repo = p.storage.repository(rid)?;
    // let node = Node::new(p.socket());
    let signer = p.signer()?;
    let mut issues = p.issues_mut(&repo)?;
    let issue_id = ObjectId::from_str(&cit.issue_id)?;
    let mut issue = issues.get_mut(&issue_id)?;
    let _er = issue.edit(cit.title, &signer)?;
    if !cit.description.is_empty() {
        let _er2 = issue.edit_description(cit.description, [], &signer)?;
    }

    Ok(String::from("{\"ok\": true}"))
}

#[cfg(test)]
mod tests {
    #[test]
    fn it_works() {
    }
}
