{strip}
<a href="/MyAccount/Home">{translate text='Your Account'}</a> <span class="divider">&raquo;</span>
{if !empty($pageTitleShort)}
	<em>{$pageTitleShort}</em>
{/if}
<span class="divider"> &raquo; </span>
{if !empty($recordCount)}
	{translate text="Showing "}	<b>{$recordStart}</b> -	<b>{$recordEnd}</b>	{translate text='of'} <b>{$recordCount}</b>
{/if}
{/strip}